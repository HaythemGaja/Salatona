import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;

public class ParseSources {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
             java.util.stream.Stream<Path> paths = Files.walk(Paths.get(args[0]))) {
            List<java.io.File> sources = new ArrayList<>();
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> sources.add(p.toFile()));
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    Arrays.asList("-proc:none", "-encoding", "UTF-8", "-source", "8"),
                    null, files.getJavaFileObjectsFromFiles(sources));
            task.parse();
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) throw new AssertionError(d.toString());
            }
            System.out.println(sources.size() + " production Java sources parsed successfully");
        }
    }
}
