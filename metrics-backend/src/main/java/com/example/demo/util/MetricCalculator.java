package com.example.demo.util;

import com.example.demo.dto.MetricResultDto;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.generic.*;


public class MetricCalculator {

    public static JavaClass parseClass(String classPath) throws IOException {
        ClassParser parser = new ClassParser(classPath);
        return parser.parse();
    }

    public static MetricResultDto calculateMetrics(JavaClass javaClass, org.apache.bcel.util.Repository repository) { // DİKKAT: .util.Repository
        String className = javaClass.getClassName();
        double tcc = calculateTCC(javaClass);
        int wmc = calculateWMC(javaClass);
        double lcom = calculateLCOM(javaClass);
        int dit = calculateDIT(javaClass, repository); // repository'yi pasla
        int cbo = calculateCBO(javaClass);
        
        Map<String, Number> cycloMetrics = calculateCycloMetrics(javaClass);
        int maxCyclo = cycloMetrics.get("max").intValue();
        double avgCyclo = cycloMetrics.get("avg").doubleValue();
        
        return new MetricResultDto(
            UUID.randomUUID(),
            className,
            tcc,
            wmc,
            lcom,
            dit,
            cbo,
            maxCyclo, // YENİ EKLENDİ
            avgCyclo  // YENİ EKLENDİ
        );
    }

    public static double calculateTCC(JavaClass javaClass) {
        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        List<MethodGen> methods = new ArrayList<>();
        for (Method m : javaClass.getMethods()) {
        	// Düzeltilmiş hali
        	if (m.isAbstract() || m.isNative()) continue; // Static metot kontrolü kaldırıldı
            if (m.getName().equals("<init>") || m.getName().equals("<clinit>")) continue;
            // visibility filtresi kaldırıldı: artık protected/private de dahil
            methods.add(new MethodGen(m, javaClass.getClassName(), cpg));
        }

        int n = methods.size();
        if (n < 2) return 0.0;

        Map<String, Set<String>> fieldAccess = new HashMap<>();
        for (MethodGen mg : methods) {
            Set<String> fields = new HashSet<>();
            InstructionList il = mg.getInstructionList();
            if (il != null) {
                for (InstructionHandle ih : il.getInstructionHandles()) {
                    Instruction inst = ih.getInstruction();
                    if (inst instanceof FieldInstruction) {
                        fields.add(((FieldInstruction) inst).getFieldName(cpg));
                    }
                }
            }
            fieldAccess.put(mg.getName(), fields);
        }

        String[] names = fieldAccess.keySet().toArray(new String[0]);
        int connected = 0, total = 0;
        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                total++;
                Set<String> f1 = fieldAccess.get(names[i]);
                Set<String> f2 = fieldAccess.get(names[j]);
                if (!Collections.disjoint(f1, f2)) connected++;
            }
        }
        return total == 0 ? 0.0 : (double) connected / total;
    }


    public static int calculateWMC(JavaClass javaClass) {
        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        int wmc = 0;
        for (Method m : javaClass.getMethods()) {
            if (m.isAbstract() || m.isNative()) {
                continue;
            }
            wmc += calculateMethodCyclomaticComplexity(m, javaClass, cpg);
        }
        return wmc;
    }

    public static double calculateLCOM(JavaClass javaClass) {
        Method[] methods = javaClass.getMethods();
        int n = methods.length;
        if (n < 2) return 0.0;

        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        Map<String, Set<String>> accessMap = new HashMap<>();
        for (Method m : methods) {
            if (m.isAbstract() || m.isNative() || Modifier.isStatic(m.getModifiers())) continue;
            if (m.getName().equals("<init>") || m.getName().equals("<clinit>")) continue;
            MethodGen mg = new MethodGen(m, javaClass.getClassName(), cpg);
            InstructionList il = mg.getInstructionList();
            Set<String> fields = new HashSet<>();
            if (il != null) {
                for (InstructionHandle ih : il.getInstructionHandles()) {
                    Instruction inst = ih.getInstruction();
                    if (inst instanceof FieldInstruction) {
                        fields.add(((FieldInstruction) inst).getFieldName(cpg));
                    }
                }
            }
            accessMap.put(mg.getName(), fields);
        }

        int P = 0, Q = 0;
        String[] names = accessMap.keySet().toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                Set<String> f1 = accessMap.get(names[i]);
                Set<String> f2 = accessMap.get(names[j]);
                if (Collections.disjoint(f1, f2)) Q++;
                else P++;
            }
        }
        return Q > P ? Q - P : 0;
    }

 // Eskisi: public static int calculateDIT(JavaClass javaClass)
 // Yenisi:
 // MetricCalculator.java
    public static int calculateDIT(JavaClass javaClass, org.apache.bcel.util.Repository repository) {
        int depth = 0;
        JavaClass current = javaClass;
        while (current.getSuperclassName() != null
               && !"java.lang.Object".equals(current.getSuperclassName())) {
            try {
                current = repository.loadClass(current.getSuperclassName());
                depth++;
            } catch (ClassNotFoundException e) {
                // !!! Hatanın kaynağını görmek için bu satırları ekledik !!!
                System.err.println("--- DIT HESAPLAMA HATASI ---");
                System.err.println("    -> Mevcut sınıf: " + javaClass.getClassName());
                System.err.println("    -> Aranan üst sınıf bulunamadı: " + current.getSuperclassName());
                System.err.println("    -> Bu hatayı almanız, classpath'in bu üst sınıfı içermediği anlamına gelir.");
                System.err.println("-----------------------------");
                // e.printStackTrace(); // Daha fazla detay için bu satırı da açabilirsiniz
                break; // Döngüyü kır ve DIT=0 ile devam et
            }
        }
        // DIT tanımı genellikle Object'e olan uzaklıktır.
        // Eğer bir sınıf doğrudan Object'ten türüyorsa (depth=0), DIT=1 olmalıdır.
        // Bu yüzden en son 1 ekliyoruz (Eğer sınıf Object değilse).
        if (!javaClass.getClassName().equals("java.lang.Object")) {
            // depth++; // Bu satırı ekleyerek DIT'nin 0 yerine 1'den başlamasını sağlayabilirsiniz.
                       // Standart tanıma göre bu daha doğru olabilir. Şimdilik yorumda kalsın.
        }
        return depth;
    }
    
    public static Map<String, Number> calculateCycloMetrics(JavaClass javaClass) {
        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        List<Integer> complexities = new ArrayList<>();
        
        // Bu döngünün, her bir metodun karmaşıklığını hesaplayıp listeye eklemesi gerekiyor.
        for (Method m : javaClass.getMethods()) {
            // Abstract ve native metotları atladığımızdan emin olalım.
            if (m.isAbstract() || m.isNative()) {
                continue;
            }
            // Her geçerli metodun karmaşıklığını listeye ekliyoruz.
            complexities.add(calculateMethodCyclomaticComplexity(m, javaClass, cpg));
        }

        int maxCyclo = 0;
        double avgCyclo = 0.0;
        double sum = 0.0;

        // Liste boş değilse hesaplama yap.
        if (!complexities.isEmpty()) {
            // En yüksek değeri bul.
            maxCyclo = Collections.max(complexities);
            
            // Ortalamayı hesaplamak için toplamı bul.
            for (int c : complexities) {
                sum += c;
            }
            avgCyclo = sum / complexities.size();
        }
        
        Map<String, Number> results = new HashMap<>();
        results.put("max", maxCyclo);
        results.put("avg", avgCyclo);
        
        return results;
    }
    

    public static int calculateCBO(JavaClass javaClass) {
        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        Set<String> coupledClasses = new HashSet<>();
        String currentClassName = javaClass.getClassName();

        // Bağımlılıkları toplayan bir yardımcı Set
        Set<String> dependencies = new HashSet<>();

        // 1. Superclass
        dependencies.add(javaClass.getSuperclassName());

        // 2. Alan (Field) Tipleri
        for (org.apache.bcel.classfile.Field f : javaClass.getFields()) {
            dependencies.add(f.getType().toString());
        }

        // 3. Metot İmzaları (Parametre ve Dönüş Tipleri)
        for (Method m : javaClass.getMethods()) {
            dependencies.add(m.getReturnType().toString());
            for (Type t : m.getArgumentTypes()) {
                dependencies.add(t.toString());
            }
        }

        // 4. Metot Gövdelerindeki Kullanımlar
        for (Method m : javaClass.getMethods()) {
            if (m.isAbstract() || m.isNative()) continue;
            MethodGen mg = new MethodGen(m, javaClass.getClassName(), cpg);
            InstructionList il = mg.getInstructionList();
            if (il == null) continue;
            for (InstructionHandle ih : il.getInstructionHandles()) {
                Instruction inst = ih.getInstruction();
                if (inst instanceof FieldInstruction) {
                    dependencies.add(((FieldInstruction) inst).getReferenceType(cpg).toString());
                } else if (inst instanceof InvokeInstruction) {
                    dependencies.add(((InvokeInstruction) inst).getReferenceType(cpg).toString());
                }
            }
        }

        // --- FİLTRELEME VE SONUÇ HESAPLAMA ---
        for (String dep : dependencies) {
            // Dizi tiplerini temizle (örn: "java.lang.String[]" -> "java.lang.String")
            String cleanDep = dep.replace("[]", "");

            // Standart Java kütüphanelerini, ilkel tipleri ve sınıfın kendisini filtrele
            if (!cleanDep.startsWith("java.") && 
                !isPrimitiveOrVoid(cleanDep) && 
                !cleanDep.equals(currentClassName)) {
                coupledClasses.add(cleanDep);
            }
        }

        return coupledClasses.size();
    }

    // İlkel tipleri kontrol etmek için yardımcı bir metot
    private static boolean isPrimitiveOrVoid(String typeName) {
        switch (typeName) {
            case "int":
            case "double":
            case "float":
            case "long":
            case "short":
            case "byte":
            case "char":
            case "boolean":
            case "void":
                return true;
            default:
                return false;
        }
    }
    
 // Bu metot, tek bir metodun Cyclomatic Karmaşıklığını hesaplar.
    private static int calculateMethodCyclomaticComplexity(Method m, JavaClass javaClass, ConstantPoolGen cpg) {
        MethodGen mg = new MethodGen(m, javaClass.getClassName(), cpg);
        InstructionList il = mg.getInstructionList();
        if (il == null) {
            return 1; // Abstract veya native metotlar için varsayılan karmaşıklık
        }

        int complexity = 1; // Her metot en az 1 yoldan oluşur.
        for (InstructionHandle ih : il.getInstructionHandles()) {
            Instruction inst = ih.getInstruction();
            
            // if, for, while gibi tüm koşullu dallanmaları sayar.
            if (inst instanceof IfInstruction) {
                complexity++;
            } 
            // switch komutunu bulduğunda özel işlem yap.
            else if (inst instanceof Select) { // 'Select', switch'i temsil eder (tableswitch veya lookupswitch)
                
                // Select komutunun kendisini al.
                Select s = (Select) inst;
                
                // Her 'case' etiketi ayrı bir yoldur.
                // getTargets() metodu, 'default' hariç tüm 'case' hedeflerini bir dizi olarak verir.
                // Bu dizinin uzunluğu kadar karmaşıklığı artır.
                complexity += s.getTargets().length;
            }
        }
        
        // try-catch bloklarını da sayalım (her catch bloğu bir karar noktasıdır)
        if (mg.getExceptionHandlers() != null) {
            complexity += mg.getExceptionHandlers().length;
        }

        return complexity;
    }
}

