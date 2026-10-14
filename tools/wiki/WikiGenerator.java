import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Genera el wiki interno (docs/wiki) a partir del código fuente. No tiene dependencias:
 *
 *     java tools/wiki/WikiGenerator.java [carpeta-del-proyecto]
 *
 * Fuentes que lee: los .kt (con su KDoc), app/schemas (Room), AndroidManifest.xml,
 * build.gradle.kts, los tests y los resultados de tests si existen. Si hay capturas crudas
 * en build/wiki-screenshots las reduce y las copia a docs/wiki/screenshots.
 *
 * La salida es determinista (sin fechas), así que un diff de git solo muestra cambios reales.
 * Para que una explicación mejore, se mejora el KDoc del código; nunca se edita el wiki a mano.
 */
public class WikiGenerator {

    static final String BASE_PKG = "com.alex.metasahorro";
    static final int SHOT_WIDTH = 540;

    static final Map<String, String> LAYER_DESC = Map.of(
        "", "Punto de entrada: la actividad que arranca la app.",
        "data", "Entidades de Room, DAO, base de datos y stores de DataStore.",
        "repository", "Lógica de negocio y cálculos de dinero.",
        "viewmodel", "Estado de la pantalla y acciones del usuario.",
        "ui", "Pantallas principales de Compose.",
        "ui.components", "Hojas inferiores y componentes reutilizables.",
        "ui.theme", "Colores, tipografía y tema Material 3 (claro, oscuro y dinámico).",
        "notifications", "Recordatorios: trabajo diario de WorkManager y notificaciones.",
        "util", "Formato de dinero y fechas.");

    // ------------------------------------------------------------------ modelo

    record Param(String name, String type, String def) {}

    record Decl(String kind, String name, String mods, String indent, String doc,
                List<String> annotations, boolean composable, List<Param> params, String ret) {
        boolean isPrivate() { return mods.contains("private"); }
        boolean isTopLevel() { return indent.isEmpty(); }
        boolean isEnum() { return mods.contains("enum"); }
        String annotation(String prefix) {
            return annotations.stream().filter(a -> a.startsWith(prefix)).findFirst().orElse(null);
        }
    }

    record Kt(Path path, String rel, String pkg, List<String> imports, List<Decl> decls, String src) {
        String fileName() { return path.getFileName().toString(); }
        int lines() { return (int) src.lines().count(); }
        String layer() { return pkg.equals(BASE_PKG) ? "" : pkg.substring(BASE_PKG.length() + 1); }
    }

    // ------------------------------------------------------------------ estado global

    static Path root, appDir, wiki, shots;
    static List<Kt> main = new ArrayList<>();
    static List<Kt> tests = new ArrayList<>();
    static List<Kt> androidTests = new ArrayList<>();
    static Map<String, String> gradle = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        appDir = root.resolve("app");
        wiki = root.resolve("docs/wiki");
        shots = wiki.resolve("screenshots");

        main = scan(appDir.resolve("src/main/java"));
        tests = scan(appDir.resolve("src/test"));
        androidTests = scan(appDir.resolve("src/androidTest"));
        if (main.isEmpty()) throw new IllegalStateException("No encontré código en " + appDir.resolve("src/main/java"));

        Files.createDirectories(wiki);
        importScreenshots();
        try (Stream<Path> old = Files.list(wiki)) {
            for (Path p : old.filter(f -> f.toString().endsWith(".md")).toList()) Files.delete(p);
        }
        readGradle();

        write("Home.md", home());
        write("Arquitectura.md", arquitectura());
        write("Modelo-de-datos.md", modeloDeDatos());
        write("Pantallas.md", pantallas());
        write("Logica-de-negocio.md", logica());
        write("Tema.md", tema());
        write("Dependencias.md", dependencias());
        write("Pruebas.md", pruebas());
        write("_Sidebar.md", sidebar());
        System.out.println("Wiki generado en " + root.relativize(wiki));
    }

    // ------------------------------------------------------------------ parser de Kotlin

    static List<Kt> scan(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            List<Path> files = s.filter(p -> p.toString().endsWith(".kt")).sorted().toList();
            List<Kt> out = new ArrayList<>();
            for (Path p : files) out.add(parseKt(p));
            return out;
        }
    }

    static final Pattern DECL = Pattern.compile(
        "^([ \\t]*)((?:(?:private|internal|public|suspend|data|enum|sealed|abstract|open|override|inline)\\s+)*)"
            + "(class|interface|object|fun)\\s+(?:<[^>]*>\\s*)?(?:[\\w.]+\\.)?(\\w+)", Pattern.MULTILINE);

    static Kt parseKt(Path p) throws IOException {
        String src = Files.readString(p).replace("\r\n", "\n");
        Matcher pk = Pattern.compile("^package\\s+([\\w.]+)", Pattern.MULTILINE).matcher(src);
        String pkg = pk.find() ? pk.group(1) : "";
        List<String> imports = new ArrayList<>();
        Matcher im = Pattern.compile("^import\\s+([\\w.*]+)", Pattern.MULTILINE).matcher(src);
        while (im.find()) imports.add(im.group(1));

        List<Decl> decls = new ArrayList<>();
        Matcher m = DECL.matcher(src);
        while (m.find()) {
            String[] before = src.substring(0, m.start()).split("\n", -1);
            int i = before.length - 2; // la última posición es la línea parcial vacía
            List<String> annotations = new ArrayList<>();
            boolean composable = false;
            while (i >= 0 && before[i].trim().startsWith("@")) {
                annotations.add(before[i].trim());
                if (before[i].contains("@Composable")) composable = true;
                i--;
            }
            String doc = "";
            if (i >= 0 && before[i].trim().endsWith("*/")) {
                int end = i;
                while (i >= 0 && !before[i].trim().startsWith("/**")) i--;
                if (i >= 0) doc = cleanDoc(Arrays.copyOfRange(before, i, end + 1));
            }
            String kind = m.group(3);
            List<Param> params = List.of();
            String ret = "";
            int open = skipSpaces(src, m.end());
            if (open < src.length() && src.charAt(open) == '(' && !kind.equals("object")) {
                int close = findClosing(src, open);
                if (close > 0) {
                    params = parseParams(src.substring(open + 1, close));
                    if (kind.equals("fun")) {
                        Matcher r = Pattern.compile("^\\s*:\\s*([^\\n{=]+)").matcher(src.substring(close + 1));
                        if (r.find()) ret = r.group(1).trim();
                    }
                }
            }
            decls.add(new Decl(kind, m.group(4), m.group(2).trim(), m.group(1), doc, annotations, composable, params, ret));
        }
        String rel = root.relativize(p).toString().replace('\\', '/');
        return new Kt(p, rel, pkg, imports, decls, src);
    }

    static String cleanDoc(String[] lines) {
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            String t = l.trim();
            t = t.replaceFirst("^/\\*\\*", "").replaceFirst("\\*/$", "").replaceFirst("^\\*", "").trim();
            if (!t.isEmpty()) out.add(t);
        }
        return String.join(" ", out);
    }

    static int skipSpaces(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    /** Índice del paréntesis que cierra al de `open`, ignorando textos entre comillas. */
    static int findClosing(String s, int open) {
        int depth = 0;
        boolean inStr = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return i;
        }
        return -1;
    }

    static boolean opens(char c) { return c == '(' || c == '[' || c == '{' || c == '<'; }
    static boolean closes(char c) { return c == ')' || c == ']' || c == '}' || c == '>'; }

    /** Divide por comas de primer nivel (ignora las de tipos genéricos, lambdas y textos). */
    static List<String> splitTop(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = 0;
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (opens(c)) depth++;
            else if (closes(c) && !(c == '>' && i > 0 && s.charAt(i - 1) == '-')) depth--;
            else if (c == ',' && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    /** Primer carácter `target` de primer nivel, o -1. */
    static int indexTop(String s, char target) {
        int depth = 0;
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (opens(c)) depth++;
            else if (closes(c) && !(c == '>' && i > 0 && s.charAt(i - 1) == '-')) depth--;
            else if (c == target && depth == 0) return i;
        }
        return -1;
    }

    static List<Param> parseParams(String s) {
        List<Param> out = new ArrayList<>();
        for (String raw : splitTop(s)) if (!raw.isBlank()) out.add(parseParam(raw));
        return out;
    }

    static Param parseParam(String raw) {
        String p = raw.trim().replaceAll("\\s+", " ");
        p = p.replaceAll("^(?:@\\w+(?:\\([^)]*\\))?\\s+)+", "");
        p = p.replaceAll("^(?:(?:private|override)\\s+)?(?:val|var)\\s+", "");
        int colon = indexTop(p, ':');
        if (colon < 0) return new Param(p, "", "");
        String name = p.substring(0, colon).trim();
        String rest = p.substring(colon + 1).trim();
        int eq = indexTop(rest, '=');
        if (eq < 0) return new Param(name, rest, "");
        return new Param(name, rest.substring(0, eq).trim(), rest.substring(eq + 1).trim());
    }

    static String signature(Decl d) {
        String ps = d.params().stream()
            .map(p -> p.name() + (p.type().isEmpty() ? "" : ": " + p.type()) + (p.def().isEmpty() ? "" : " = " + p.def()))
            .collect(Collectors.joining(", "));
        String head = (d.mods().contains("suspend") ? "suspend " : "") + "fun " + d.name() + "(";
        String tail = ")" + (d.ret().isEmpty() ? "" : ": " + d.ret());
        if ((head + ps + tail).length() <= 100) return head + ps + tail;
        String sep = ",\n    ";
        return head + "\n    " + d.params().stream()
            .map(p -> p.name() + (p.type().isEmpty() ? "" : ": " + p.type()) + (p.def().isEmpty() ? "" : " = " + p.def()))
            .collect(Collectors.joining(sep)) + "\n" + tail;
    }

    // ------------------------------------------------------------------ utilidades de Markdown

    static String esc(String s) { return s.replace("|", "\\|").replace("\n", " "); }

    static String table(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|").append(" --- |".repeat(headers.size())).append("\n");
        for (List<String> r : rows) {
            sb.append("| ").append(r.stream().map(WikiGenerator::esc).collect(Collectors.joining(" | "))).append(" |\n");
        }
        return sb.append("\n").toString();
    }

    static String firstSentence(String doc) {
        if (doc.isEmpty()) return "";
        int i = doc.indexOf(". ");
        return i < 0 ? doc : doc.substring(0, i + 1);
    }

    static String srcLink(Kt k) { return "[`" + k.fileName() + "`](../../" + k.rel() + ")"; }

    static StringBuilder page(String title, String intro) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- AUTO-GENERADO por tools/wiki/WikiGenerator.java. No editar a mano. -->\n");
        sb.append("# ").append(title).append("\n\n");
        sb.append("> Página generada automáticamente desde el código. Para cambiarla, mejora el KDoc del código y corre `gradlew generateWiki`.\n\n");
        if (!intro.isEmpty()) sb.append(intro).append("\n\n");
        return sb;
    }

    static void write(String name, CharSequence content) throws IOException {
        Files.writeString(wiki.resolve(name), content.toString().replaceAll("\n{3,}", "\n\n").stripTrailing() + "\n");
    }

    static List<String> shotsFor(String composable) throws IOException {
        if (!Files.isDirectory(shots)) return List.of();
        try (Stream<Path> s = Files.list(shots)) {
            return s.map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".png") && (n.equals(composable + ".png") || n.startsWith(composable + "__")))
                .sorted(Comparator.comparing((String n) -> !n.equals(composable + ".png")).thenComparing(n -> n))
                .toList();
        }
    }

    static String img(String file, int width) {
        String alt = file.replace(".png", "").replace("__", " · ");
        return "<img src=\"screenshots/" + file + "\" alt=\"" + alt + "\" width=\"" + width + "\">";
    }

    /** `expected`: si es una vista completa (pantalla u hoja), avisa cuando falta su captura; los componentes sueltos no la necesitan. */
    static String shotGallery(String composable, boolean expected) throws IOException {
        List<String> files = shotsFor(composable);
        if (files.isEmpty()) {
            return expected
                ? "_Sin captura todavía. Corre `tools/wiki/capture-screenshots.ps1` con el emulador disponible._\n\n"
                : "";
        }
        StringBuilder sb = new StringBuilder("<table><tr>\n");
        for (String f : files) {
            String caption = f.equals(composable + ".png") ? "" : f.substring((composable + "__").length(), f.length() - 4).replace('-', ' ');
            sb.append("<td align=\"center\">").append(img(f, 240));
            if (!caption.isEmpty()) sb.append("<br><sub>").append(caption).append("</sub>");
            sb.append("</td>\n");
        }
        return sb.append("</tr></table>\n\n").toString();
    }

    // ------------------------------------------------------------------ capturas

    static void importScreenshots() throws IOException {
        Path raw = root.resolve("build/wiki-screenshots");
        if (!Files.isDirectory(raw)) return;
        List<Path> pngs;
        try (Stream<Path> s = Files.list(raw)) { pngs = s.filter(p -> p.toString().endsWith(".png")).sorted().toList(); }
        if (pngs.isEmpty()) return;
        Files.createDirectories(shots);
        try (Stream<Path> old = Files.list(shots)) {
            for (Path p : old.filter(f -> f.toString().endsWith(".png")).toList()) Files.delete(p);
        }
        for (Path p : pngs) {
            BufferedImage img = ImageIO.read(p.toFile());
            if (img == null) continue;
            ImageIO.write(scale(img, SHOT_WIDTH), "png", shots.resolve(p.getFileName()).toFile());
        }
        System.out.println("Capturas importadas: " + pngs.size());
    }

    static BufferedImage scale(BufferedImage src, int targetW) {
        BufferedImage cur = src;
        while (cur.getWidth() > targetW) {
            int w = Math.max(targetW, cur.getWidth() / 2);
            int h = Math.round(cur.getHeight() * (w / (float) cur.getWidth()));
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, w, h, null);
            g.dispose();
            cur = next;
        }
        return cur;
    }

    // ------------------------------------------------------------------ lectura de Gradle / manifest

    static String read(Path p) throws IOException { return Files.isRegularFile(p) ? Files.readString(p).replace("\r\n", "\n") : ""; }

    static String find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "";
    }

    static void readGradle() throws IOException {
        String app = read(appDir.resolve("build.gradle.kts"));
        gradle.put("namespace", find(app, "namespace\\s*=\\s*\"([^\"]+)\""));
        gradle.put("applicationId", find(app, "applicationId\\s*=\\s*\"([^\"]+)\""));
        gradle.put("compileSdk", find(app, "compileSdk\\s*=\\s*(\\d+)"));
        gradle.put("minSdk", find(app, "minSdk\\s*=\\s*(\\d+)"));
        gradle.put("targetSdk", find(app, "targetSdk\\s*=\\s*(\\d+)"));
        gradle.put("versionName", find(app, "versionName\\s*=\\s*\"([^\"]+)\""));
        gradle.put("versionCode", find(app, "versionCode\\s*=\\s*(\\d+)"));
    }

    static String appName() throws IOException {
        String n = find(read(appDir.resolve("src/main/res/values/strings.xml")), "name=\"app_name\">([^<]+)<");
        return n.isEmpty() ? "App" : n;
    }

    // ------------------------------------------------------------------ páginas

    static List<Decl> publicComposables() {
        List<Decl> out = new ArrayList<>();
        for (Kt k : main) {
            if (k.layer().equals("ui.theme")) continue;
            for (Decl d : k.decls()) if (d.composable() && d.isTopLevel() && !d.isPrivate() && d.kind().equals("fun")) out.add(d);
        }
        return out;
    }

    static Kt fileOf(Decl d) {
        for (Kt k : main) if (k.decls().contains(d)) return k;
        return null;
    }

    static List<Decl> entities() {
        return main.stream().flatMap(k -> k.decls().stream()).filter(d -> d.annotation("@Entity") != null).toList();
    }

    static long countTests(List<Kt> files) {
        return files.stream().mapToLong(k -> Pattern.compile("@Test\\b").matcher(k.src()).results().count()).sum();
    }

    static String home() throws IOException {
        String name = appName();
        StringBuilder sb = page("Wiki de " + name, "Documentación interna de la app Android **" + name + "**: arquitectura, datos, pantallas y lógica.");
        Kt activity = main.stream().filter(k -> k.fileName().equals("MainActivity.kt")).findFirst().orElse(null);
        if (activity != null) {
            Decl d = activity.decls().stream().filter(x -> x.name().equals("MainActivity")).findFirst().orElse(null);
            if (d != null && !d.doc().isEmpty()) sb.append("**Punto de entrada.** ").append(d.doc()).append(" (").append(srcLink(activity)).append(")\n\n");
        }
        if (!shotsFor("MainScreen").isEmpty()) sb.append(img("MainScreen.png", 280)).append("\n\n");

        sb.append("## Resumen\n\n");
        int mainLines = main.stream().mapToInt(Kt::lines).sum();
        long composables = publicComposables().size();
        sb.append(table(List.of("Dato", "Valor"), List.of(
            List.of("Aplicación (applicationId)", "`" + gradle.get("applicationId") + "`"),
            List.of("Versión", gradle.get("versionName") + " (código " + gradle.get("versionCode") + ")"),
            List.of("SDK mínimo / objetivo / compilación", gradle.get("minSdk") + " / " + gradle.get("targetSdk") + " / " + gradle.get("compileSdk")),
            List.of("Archivos Kotlin (main)", main.size() + " (" + mainLines + " líneas)"),
            List.of("Pantallas y componentes públicos", String.valueOf(composables)),
            List.of("Tablas de Room", String.valueOf(entities().size())),
            List.of("Tests unitarios / instrumentados", countTests(tests) + " / " + countTests(androidTests)))));

        sb.append("## Contenido\n\n");
        sb.append("- [Arquitectura](Arquitectura.md): capas, dependencias entre paquetes y manifest.\n");
        sb.append("- [Modelo de datos](Modelo-de-datos.md): tablas de Room, enums, DataStore y consultas.\n");
        sb.append("- [Pantallas](Pantallas.md): cada vista con su captura, parámetros y cómo se abre.\n");
        sb.append("- [Lógica de negocio](Logica-de-negocio.md): operaciones del repositorio, ViewModel y cálculos.\n");
        sb.append("- [Tema](Tema.md): colores y temas de acento.\n");
        sb.append("- [Dependencias](Dependencias.md): librerías y versiones.\n");
        sb.append("- [Pruebas](Pruebas.md): tests y su último resultado.\n\n");

        sb.append("## Cómo se genera este wiki\n\n");
        sb.append("- **Texto:** `gradlew generateWiki` corre `tools/wiki/WikiGenerator.java`, que lee el código, el KDoc, el esquema de Room, el manifest y Gradle. No requiere emulador.\n");
        sb.append("- **Capturas:** `tools/wiki/capture-screenshots.ps1` levanta el emulador, corre la prueba instrumentada `WikiScreenshotsTest` (carga datos de ejemplo y recorre la app) y luego regenera el wiki.\n");
        sb.append("- **Regla:** no se edita nada en `docs/wiki/` a mano. Si una explicación está floja, se mejora el KDoc del código.\n");
        return sb.toString();
    }

    static String arquitectura() throws IOException {
        StringBuilder sb = page("Arquitectura", "App de un solo módulo (`app`) con capas separadas por paquete. Los datos viven 100% en el teléfono.");

        // Capas y dependencias
        Map<String, List<Kt>> byLayer = new TreeMap<>();
        for (Kt k : main) byLayer.computeIfAbsent(k.layer(), x -> new ArrayList<>()).add(k);
        Set<String> edges = new TreeSet<>();
        for (Kt k : main) {
            for (String imp : k.imports()) {
                // `R` (los recursos generados) no es una dependencia de diseño entre capas.
                if (!imp.startsWith(BASE_PKG + ".") || imp.equals(BASE_PKG + ".R")) continue;
                String pkg = imp.endsWith(".*") ? imp.substring(0, imp.length() - 2) : imp.substring(0, imp.lastIndexOf('.'));
                String target = pkg.equals(BASE_PKG) ? "" : pkg.substring(BASE_PKG.length() + 1);
                if (!target.equals(k.layer()) && byLayer.containsKey(target)) edges.add(k.layer() + "->" + target);
            }
        }
        sb.append("## Capas y quién usa a quién\n\n```mermaid\ngraph LR\n");
        for (String layer : byLayer.keySet()) sb.append("  ").append(nodeId(layer)).append("[\"").append(layerLabel(layer)).append("\"]\n");
        for (String e : edges) {
            String[] ab = e.split("->", -1);
            sb.append("  ").append(nodeId(ab[0])).append(" --> ").append(nodeId(ab[1])).append("\n");
        }
        sb.append("```\n\n");
        sb.append("_Las flechas salen de los `import` reales de cada archivo._\n\n");

        List<List<String>> rows = new ArrayList<>();
        for (var e : byLayer.entrySet()) {
            rows.add(List.of("`" + layerLabel(e.getKey()) + "`", String.valueOf(e.getValue().size()),
                String.valueOf(e.getValue().stream().mapToInt(Kt::lines).sum()),
                LAYER_DESC.getOrDefault(e.getKey(), "_(sin descripción: agregarla en `LAYER_DESC` del generador)_")));
        }
        sb.append(table(List.of("Paquete", "Archivos", "Líneas", "Responsabilidad"), rows));

        sb.append("## Archivos por capa\n\n");
        for (var e : byLayer.entrySet()) {
            sb.append("### `").append(layerLabel(e.getKey())).append("`\n\n");
            List<List<String>> fr = new ArrayList<>();
            for (Kt k : e.getValue()) {
                List<Decl> tops = k.decls().stream().filter(d -> d.isTopLevel() && !d.isPrivate()).toList();
                String names = tops.stream().map(d -> "`" + d.name() + "`").collect(Collectors.joining(", "));
                String doc = tops.stream().map(d -> firstSentence(d.doc())).filter(s -> !s.isEmpty()).findFirst().orElse("");
                fr.add(List.of(srcLink(k), names.isEmpty() ? "—" : names, doc));
            }
            sb.append(table(List.of("Archivo", "Declara", "Qué hace"), fr));
        }

        // Manifest
        String manifest = read(appDir.resolve("src/main/AndroidManifest.xml"));
        sb.append("## Manifest\n\n");
        List<String> perms = Pattern.compile("<uses-permission[^>]*android:name=\"([^\"]+)\"").matcher(manifest).results().map(r -> r.group(1)).toList();
        sb.append("- **Permisos:** ").append(perms.isEmpty() ? "ninguno" : perms.stream().map(p -> "`" + p + "`").collect(Collectors.joining(", "))).append("\n");
        sb.append("- **Etiqueta:** `").append(find(manifest, "android:label=\"([^\"]+)\"")).append("`\n");
        sb.append("- **allowBackup:** `").append(find(manifest, "android:allowBackup=\"([^\"]+)\"")).append("`\n");
        Matcher act = Pattern.compile("<activity[^>]*android:name=\"([^\"]+)\"[^>]*android:exported=\"([^\"]+)\"").matcher(manifest);
        while (act.find()) sb.append("- **Actividad:** `").append(act.group(1)).append("` (exported=").append(act.group(2)).append(")\n");
        return sb.toString();
    }

    static String nodeId(String layer) { return layer.isEmpty() ? "app" : layer.replace('.', '_'); }
    static String layerLabel(String layer) { return layer.isEmpty() ? "app (raíz)" : layer; }

    // ---- datos

    record Table(String name, String sql, List<String[]> columns, Set<String> pk) {}

    static String decodeJson(String s) {
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1), 16))));
        m.appendTail(sb);
        return sb.toString().replace("\\\"", "\"").replace("\\\\", "\\");
    }

    static String modeloDeDatos() throws IOException {
        StringBuilder sb = page("Modelo de datos", "Todo se guarda localmente con Room (SQLite) y DataStore. No hay servidor.");

        // Esquema exportado por Room (el de versión más alta)
        Path schemaFile = null;
        int bestVersion = -1;
        Path schemas = appDir.resolve("schemas");
        if (Files.isDirectory(schemas)) {
            try (Stream<Path> s = Files.walk(schemas)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".json")).toList()) {
                    String n = p.getFileName().toString().replace(".json", "");
                    if (n.matches("\\d+") && Integer.parseInt(n) > bestVersion) { bestVersion = Integer.parseInt(n); schemaFile = p; }
                }
            }
        }
        List<Table> tables = new ArrayList<>();
        if (schemaFile != null) {
            String json = Files.readString(schemaFile);
            sb.append("**Versión del esquema:** ").append(bestVersion).append(" · **identityHash:** `")
                .append(find(json, "\"identityHash\":\\s*\"([^\"]+)\"")).append("`\n\n");
            Matcher t = Pattern.compile("\"tableName\":\\s*\"(\\w+)\",\\s*\"createSql\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
            while (t.find()) {
                String name = t.group(1);
                String sql = decodeJson(t.group(2)).replace("${TABLE_NAME}", name);
                List<String[]> cols = new ArrayList<>();
                int paren = sql.indexOf('(');
                Matcher c = Pattern.compile("`(\\w+)` (INTEGER|TEXT|REAL|BLOB)((?: PRIMARY KEY AUTOINCREMENT)?)((?: NOT NULL)?)").matcher(sql.substring(Math.max(paren, 0)));
                Set<String> pk = new LinkedHashSet<>();
                while (c.find()) {
                    cols.add(new String[]{c.group(1), c.group(2), c.group(4).isEmpty() ? "opcional" : ""});
                    if (!c.group(3).isEmpty()) pk.add(c.group(1));
                }
                Matcher pm = Pattern.compile("PRIMARY KEY\\(([^)]*)\\)").matcher(sql);
                if (pm.find()) for (String col : pm.group(1).split(",")) pk.add(col.replace("`", "").trim());
                tables.add(new Table(name, sql, cols, pk));
            }
            if (!tables.isEmpty()) {
                sb.append("## Diagrama\n\n```mermaid\nerDiagram\n");
                for (Table tb : tables) {
                    sb.append("  ").append(tb.name()).append(" {\n");
                    for (String[] col : tb.columns()) {
                        sb.append("    ").append(col[1]).append(" ").append(col[0]);
                        if (tb.pk().contains(col[0])) sb.append(" PK");
                        if (!col[2].isEmpty()) sb.append(" \"opcional\"");
                        sb.append("\n");
                    }
                    sb.append("  }\n");
                }
                sb.append("```\n\n");
                boolean fk = tables.stream().anyMatch(tb -> tb.sql().contains("FOREIGN KEY"));
                sb.append(fk ? "_Hay claves foráneas entre tablas (ver SQL)._\n\n"
                    : "_Las tablas son independientes: no hay claves foráneas._\n\n");
            }
        } else {
            sb.append("_No hay esquema exportado en `app/schemas`. Activa `exportSchema` y compila._\n\n");
        }

        sb.append("## Tablas\n\n");
        for (Decl e : entities()) {
            Kt file = fileOf(e);
            String tableName = find(e.annotation("@Entity"), "tableName\\s*=\\s*\"(\\w+)\"");
            sb.append("### `").append(tableName.isEmpty() ? e.name() : tableName).append("` (`").append(e.name()).append("`)\n\n");
            if (!e.doc().isEmpty()) sb.append(e.doc()).append("\n\n");
            sb.append("Código: ").append(srcLink(file)).append("\n\n");
            List<List<String>> rows = new ArrayList<>();
            for (Param p : e.params()) {
                rows.add(List.of("`" + p.name() + "`", "`" + p.type() + "`", p.type().endsWith("?") ? "sí" : "no", p.def().isEmpty() ? "—" : "`" + p.def() + "`"));
            }
            sb.append(table(List.of("Campo", "Tipo Kotlin", "Nulo", "Por defecto"), rows));
            Table tb = tables.stream().filter(x -> x.name().equals(tableName)).findFirst().orElse(null);
            if (tb != null) sb.append("```sql\n").append(tb.sql()).append("\n```\n\n");
        }

        // Enums
        sb.append("## Enums\n\n");
        for (Kt k : main) {
            Matcher m = Pattern.compile("enum class (\\w+)\\s*(?:\\(([^)]*)\\))?\\s*\\{([^}]*)\\}").matcher(k.src());
            while (m.find()) {
                Decl d = k.decls().stream().filter(x -> x.name().equals(m.group(1)) && x.isEnum()).findFirst().orElse(null);
                if (d == null || d.isPrivate()) continue;
                List<String> entries = new ArrayList<>();
                Matcher en = Pattern.compile("(\\w+)(\\([^)]*\\))?").matcher(m.group(3).split(";")[0]);
                while (en.find()) entries.add("`" + en.group(1) + (en.group(2) == null ? "" : en.group(2)) + "`");
                sb.append("- **`").append(d.name()).append("`** (").append(srcLink(k)).append("): ").append(String.join(", ", entries));
                if (!d.doc().isEmpty()) sb.append(" — ").append(d.doc());
                sb.append("\n");
            }
        }
        sb.append("\n");

        // DataStore
        List<List<String>> ds = new ArrayList<>();
        for (Kt k : main) {
            String store = find(k.src(), "preferencesDataStore\\(name\\s*=\\s*\"([^\"]+)\"\\)");
            Matcher keys = Pattern.compile("(\\w+)PreferencesKey\\(\"([^\"]+)\"\\)").matcher(k.src());
            while (keys.find()) ds.add(List.of(srcLink(k), "`" + store + "`", "`" + keys.group(2) + "`", keys.group(1)));
        }
        if (!ds.isEmpty()) {
            sb.append("## Preferencias (DataStore)\n\n").append(table(List.of("Archivo", "DataStore", "Clave", "Tipo"), ds));
        }

        // DAO
        sb.append("## Consultas (DAO)\n\n");
        for (Kt k : main) {
            if (k.decls().stream().noneMatch(d -> d.kind().equals("interface") && d.annotation("@Dao") != null)) continue;
            sb.append("Origen: ").append(srcLink(k)).append("\n\n");
            String section = "General";
            String pending = null, sql = "";
            Map<String, List<List<String>>> grouped = new LinkedHashMap<>();
            for (String line : k.src().split("\n")) {
                String t = line.trim();
                Matcher sec = Pattern.compile("^//\\s*-+\\s*(.*?)\\s*-+$").matcher(t);
                if (sec.find()) { section = sec.group(1); continue; }
                Matcher q = Pattern.compile("^@Query\\(\"(.*)\"\\)$").matcher(t);
                if (q.find()) { pending = "Query"; sql = q.group(1); continue; }
                Matcher a = Pattern.compile("^@(Insert|Update|Delete)\\b").matcher(t);
                if (a.find()) { pending = a.group(1); sql = ""; continue; }
                Matcher f = Pattern.compile("^(suspend )?fun (\\w+)\\((.*?)\\)(?:: (.+))?$").matcher(t);
                if (f.find() && pending != null) {
                    grouped.computeIfAbsent(section, x -> new ArrayList<>()).add(List.of("`" + f.group(2) + "`", pending,
                        f.group(1) != null ? "suspend" : "Flow", sql.isEmpty() ? "—" : "`" + sql + "`"));
                    pending = null;
                }
            }
            for (var g : grouped.entrySet()) {
                sb.append("**").append(g.getKey()).append("**\n\n").append(table(List.of("Función", "Tipo", "Ejecución", "SQL"), g.getValue()));
            }
        }
        return sb.toString();
    }

    // ---- pantallas

    static String pantallas() throws IOException {
        StringBuilder sb = page("Pantallas", "Una pantalla principal y varias hojas inferiores (bottom sheets) que se abren encima. Las capturas se toman en el emulador con datos de ejemplo.");

        // Estados de hoja y cómo se abren
        Kt mainScreen = main.stream().filter(k -> k.fileName().equals("MainScreen.kt")).findFirst().orElse(null);
        if (mainScreen != null) {
            Map<String, String> stateToComposable = new LinkedHashMap<>();
            Matcher w = Pattern.compile("((?:SheetType\\.\\w+\\s*,?\\s*)+)->\\s*(?:[\\w?.]+\\s*\\{\\s*\\w+\\s*->\\s*)?(\\w+)\\(").matcher(mainScreen.src());
            while (w.find()) {
                Matcher st = Pattern.compile("SheetType\\.(\\w+)").matcher(w.group(1));
                while (st.find()) stateToComposable.put(st.group(1), w.group(2));
            }
            List<List<String>> rows = new ArrayList<>();
            Matcher tr = Pattern.compile("(\\w+)\\s*=\\s*\\{[^{}]*?sheet\\s*=\\s*SheetType\\.(\\w+)").matcher(mainScreen.src());
            while (tr.find()) {
                String state = tr.group(2);
                if (state.equals("NONE") || !stateToComposable.containsKey(state)) continue;
                rows.add(List.of("`" + tr.group(1) + "`", "`" + state + "`", "[" + stateToComposable.get(state) + "](#" + stateToComposable.get(state).toLowerCase() + ")"));
            }
            if (!rows.isEmpty()) {
                sb.append("## Cómo se abre cada vista\n\n");
                sb.append("`MainScreen` guarda un estado `SheetType`; cada callback de abajo lo cambia y se muestra la hoja correspondiente.\n\n");
                sb.append(table(List.of("Callback", "Estado", "Se muestra"), rows));
            }
        }

        List<Decl> all = publicComposables();
        record Group(String title, java.util.function.Predicate<Decl> test) {}
        List<Group> groups = List.of(
            new Group("Pantallas", d -> fileOf(d).fileName().endsWith("Screen.kt")),
            new Group("Hojas inferiores", d -> d.name().endsWith("Sheet")),
            new Group("Componentes", d -> !fileOf(d).fileName().endsWith("Screen.kt") && !d.name().endsWith("Sheet")));
        for (Group g : groups) {
            List<Decl> ds = all.stream().filter(g.test()).sorted(Comparator.comparing(Decl::name)).toList();
            if (ds.isEmpty()) continue;
            sb.append("## ").append(g.title()).append("\n\n");
            for (Decl d : ds) {
                sb.append("### ").append(d.name()).append("\n\n");
                if (!d.doc().isEmpty()) sb.append(d.doc()).append("\n\n");
                sb.append("Código: ").append(srcLink(fileOf(d))).append("\n\n");
                sb.append(shotGallery(d.name(), !g.title().equals("Componentes")));
                if (!d.params().isEmpty()) {
                    List<List<String>> pr = new ArrayList<>();
                    for (Param p : d.params()) pr.add(List.of("`" + p.name() + "`", "`" + p.type() + "`", p.def().isEmpty() ? "—" : "`" + p.def() + "`"));
                    sb.append(table(List.of("Parámetro", "Tipo", "Por defecto"), pr));
                }
            }
        }
        // Capturas sueltas que no corresponden a un composable (p. ej. diálogos)
        if (Files.isDirectory(shots)) {
            Set<String> names = all.stream().map(Decl::name).collect(Collectors.toSet());
            List<String> extra;
            try (Stream<Path> s = Files.list(shots)) {
                extra = s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png"))
                    .filter(n -> names.stream().noneMatch(x -> n.equals(x + ".png") || n.startsWith(x + "__"))).sorted().toList();
            }
            if (!extra.isEmpty()) {
                sb.append("## Otras capturas\n\n<table><tr>\n");
                for (String f : extra) sb.append("<td align=\"center\">").append(img(f, 240)).append("<br><sub>").append(f.replace(".png", "").replace("__", " · ").replace('-', ' ')).append("</sub></td>\n");
                sb.append("</tr></table>\n\n");
            }
        }
        return sb.toString();
    }

    // ---- lógica

    static String logica() {
        StringBuilder sb = page("Lógica de negocio", "Operaciones sobre metas y dinero. Las que tocan el pozo corren dentro de una transacción de Room y leen el saldo actual de la base.");
        for (String file : List.of("MetasRepository.kt", "MoneyMath.kt", "Achievements.kt", "ReminderPlanner.kt", "Reminders.kt", "MetasViewModel.kt")) {
            Kt k = main.stream().filter(x -> x.fileName().equals(file)).findFirst().orElse(null);
            if (k == null) continue;
            sb.append("## ").append(file.replace(".kt", "")).append("\n\n").append("Código: ").append(srcLink(k)).append("\n\n");
            for (Decl cls : k.decls()) {
                if (cls.isTopLevel() && (cls.kind().equals("class") || cls.kind().equals("object")) && !cls.doc().isEmpty()) sb.append(cls.doc()).append("\n\n");
            }
            for (Decl d : k.decls()) {
                boolean member = d.indent().equals("    ") && !d.isPrivate();
                boolean top = d.isTopLevel() && !d.isPrivate();
                boolean isRepoOrVm = file.equals("MetasRepository.kt") || file.equals("MetasViewModel.kt") || file.equals("Reminders.kt");
                if (!d.kind().equals("fun") || !(isRepoOrVm ? member : top)) continue;
                sb.append("### `").append(d.name()).append("`\n\n");
                sb.append(d.doc().isEmpty() ? "_Sin documentación en el código (agregar KDoc)._" : d.doc()).append("\n\n");
                sb.append("```kotlin\n").append(signature(d)).append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    // ---- tema

    static String tema() throws IOException {
        StringBuilder sb = page("Tema", "La app es oscura y cambia solo el color de acento. Los valores salen de `Theme.kt`.");
        Kt theme = main.stream().filter(k -> k.fileName().equals("Theme.kt")).findFirst().orElse(null);
        if (theme == null) return sb.toString();

        List<String[]> colors = new ArrayList<>();
        Matcher c = Pattern.compile("val (\\w+)\\s*=\\s*Color\\(0x([0-9A-Fa-f]{8})\\)").matcher(theme.src());
        while (c.find()) colors.add(new String[]{c.group(1), "#" + c.group(2).substring(2).toUpperCase()});
        Matcher a = Pattern.compile("AccentTheme\\.(\\w+)\\s*->\\s*AccentPair\\(Color\\(0x(\\w{8})\\),\\s*Color\\(0x(\\w{8})\\)\\)").matcher(theme.src());
        List<String[]> accents = new ArrayList<>();
        while (a.find()) accents.add(new String[]{a.group(1), "#" + a.group(2).substring(2).toUpperCase(), "#" + a.group(3).substring(2).toUpperCase()});

        // paleta en SVG
        List<String[]> swatches = new ArrayList<>(colors);
        for (String[] ac : accents) { swatches.add(new String[]{"accent " + ac[0], ac[1]}); }
        Files.createDirectories(wiki.resolve("assets"));
        StringBuilder svg = new StringBuilder();
        int cols = 4, cell = 150, h = 74;
        int rowsN = (swatches.size() + cols - 1) / cols;
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(cols * cell).append("\" height=\"").append(rowsN * h).append("\" font-family=\"sans-serif\" font-size=\"11\">\n");
        for (int i = 0; i < swatches.size(); i++) {
            int x = (i % cols) * cell, y = (i / cols) * h;
            String hex = swatches.get(i)[1];
            int rgb = Integer.parseInt(hex.substring(1), 16);
            double lum = 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
            svg.append("<rect x=\"").append(x + 4).append("\" y=\"").append(y + 4).append("\" width=\"").append(cell - 8).append("\" height=\"").append(h - 8)
                .append("\" rx=\"10\" fill=\"").append(hex).append("\" stroke=\"#888\" stroke-opacity=\"0.4\"/>\n");
            svg.append("<text x=\"").append(x + 14).append("\" y=\"").append(y + 30).append("\" fill=\"").append(lum > 140 ? "#111" : "#fff").append("\">").append(swatches.get(i)[0]).append("</text>\n");
            svg.append("<text x=\"").append(x + 14).append("\" y=\"").append(y + 50).append("\" fill=\"").append(lum > 140 ? "#111" : "#fff").append("\" opacity=\"0.8\">").append(hex).append("</text>\n");
        }
        svg.append("</svg>\n");
        Files.writeString(wiki.resolve("assets/palette.svg"), svg.toString());

        sb.append("![Paleta](assets/palette.svg)\n\n## Colores base\n\n");
        List<List<String>> rows = new ArrayList<>();
        for (String[] col : colors) rows.add(List.of("`" + col[0] + "`", "`" + col[1] + "`"));
        sb.append(table(List.of("Nombre", "Hex"), rows));

        sb.append("## Acentos\n\n");
        rows = new ArrayList<>();
        for (String[] ac : accents) rows.add(List.of("`" + ac[0] + "`", "`" + ac[1] + "`", "`" + ac[2] + "`"));
        sb.append(table(List.of("Tema", "Acento", "Acento atenuado"), rows));
        sb.append(shotGallery("SettingsSheet", false));
        sb.append("Código: ").append(srcLink(theme)).append("\n");
        return sb.toString();
    }

    // ---- dependencias

    static String dependencias() throws IOException {
        StringBuilder sb = page("Dependencias", "Se leen de `app/build.gradle.kts` y `build.gradle.kts`.");
        sb.append(table(List.of("Ajuste", "Valor"), List.of(
            List.of("namespace", "`" + gradle.get("namespace") + "`"),
            List.of("compileSdk", gradle.get("compileSdk")),
            List.of("minSdk", gradle.get("minSdk")),
            List.of("targetSdk", gradle.get("targetSdk")),
            List.of("versión", gradle.get("versionName") + " (" + gradle.get("versionCode") + ")"))));

        String rootGradle = read(root.resolve("build.gradle.kts"));
        List<List<String>> plugins = new ArrayList<>();
        Matcher p = Pattern.compile("id\\(\"([^\"]+)\"\\)\\s*version\\s*\"([^\"]+)\"").matcher(rootGradle);
        while (p.find()) plugins.add(List.of("`" + p.group(1) + "`", p.group(2)));
        if (!plugins.isEmpty()) sb.append("## Plugins\n\n").append(table(List.of("Plugin", "Versión"), plugins));

        String app = read(appDir.resolve("build.gradle.kts"));
        List<List<String>> deps = new ArrayList<>();
        Matcher d = Pattern.compile("^\\s*(implementation|ksp|testImplementation|debugImplementation|androidTestImplementation)\\((platform\\()?\"([^\"]+)\"\\)?\\)", Pattern.MULTILINE).matcher(app);
        while (d.find()) {
            String[] parts = d.group(3).split(":");
            String version = parts.length > 2 ? parts[2] : (d.group(2) != null ? "BOM" : "(la define el BOM)");
            deps.add(List.of(d.group(1), "`" + parts[0] + ":" + parts[1] + "`", version));
        }
        sb.append("## Librerías\n\n").append(table(List.of("Configuración", "Artefacto", "Versión"), deps));
        return sb.toString();
    }

    // ---- pruebas

    static String pruebas() throws IOException {
        StringBuilder sb = page("Pruebas", "Tests unitarios (JVM) e instrumentados (emulador o dispositivo).");

        // resultados del último `gradlew testDebugUnitTest` y de la última corrida en el emulador
        Map<String, String> results = new HashMap<>();
        for (String dir : List.of("build/test-results/testDebugUnitTest", "build/outputs/androidTest-results/connected")) {
            Path resDir = appDir.resolve(dir);
            if (!Files.isDirectory(resDir)) continue;
            try (Stream<Path> s = Files.walk(resDir)) {
                for (Path x : s.filter(f -> f.toString().endsWith(".xml")).toList()) {
                    String xml = Files.readString(x);
                    Matcher tc = Pattern.compile("<testcase name=\"([^\"]+)\" classname=\"([^\"]+)\"[^>]*?(/>|>(.*?)</testcase>)", Pattern.DOTALL).matcher(xml);
                    while (tc.find()) {
                        String body = tc.group(4) == null ? "" : tc.group(4);
                        results.put(tc.group(2) + "#" + tc.group(1), body.contains("<failure") || body.contains("<error") ? "❌" : body.contains("<skipped") ? "⏭" : "✅");
                    }
                }
            }
        }

        for (var set : List.of(Map.entry("Unitarios (`app/src/test`)", tests), Map.entry("Instrumentados (`app/src/androidTest`)", androidTests))) {
            if (set.getValue().isEmpty()) continue;
            sb.append("## ").append(set.getKey()).append("\n\n");
            for (Kt k : set.getValue()) {
                Matcher t = Pattern.compile("@Test\\b[^\\n]*\\n\\s*fun (\\w+)").matcher(k.src());
                List<List<String>> rows = new ArrayList<>();
                while (t.find()) {
                    String res = results.getOrDefault(k.pkg() + "." + k.fileName().replace(".kt", "") + "#" + t.group(1), "—");
                    rows.add(List.of("`" + t.group(1) + "`", res));
                }
                if (rows.isEmpty()) continue;
                sb.append("**").append(k.fileName().replace(".kt", "")).append("** (").append(srcLink(k)).append(")\n\n");
                sb.append(table(List.of("Test", "Último resultado"), rows));
            }
        }
        sb.append("_✅ pasó · ❌ falló · — sin resultado (correr `gradlew :app:testDebugUnitTest` antes de generar el wiki)._\n");
        return sb.toString();
    }

    static String sidebar() {
        return "<!-- AUTO-GENERADO por tools/wiki/WikiGenerator.java. No editar a mano. -->\n"
            + "- [Inicio](Home.md)\n- [Arquitectura](Arquitectura.md)\n- [Modelo de datos](Modelo-de-datos.md)\n"
            + "- [Pantallas](Pantallas.md)\n- [Lógica de negocio](Logica-de-negocio.md)\n- [Tema](Tema.md)\n"
            + "- [Dependencias](Dependencias.md)\n- [Pruebas](Pruebas.md)\n";
    }
}
