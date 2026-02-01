package com.example.demo.service.impl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AnalysisResponseDto;
import com.example.demo.dto.AnalysisSummaryDto;
import com.example.demo.dto.MetricResultDto;
import com.example.demo.dto.VisualizeRequest;
import com.example.demo.dto.VisualizeResponseDTO;
import com.example.demo.dto.VisualizeResultDTO;
import com.example.demo.model.AnalysisEntity;
import com.example.demo.model.MetricResultEntity;
import com.example.demo.repository.AnalysisRepository;
import com.example.demo.service.AnalysisService;
import com.example.demo.util.MetricCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
	
    private final AnalysisRepository analysisRepository;

 // AnalysisServiceImpl.java

 // ... importlar ve sınıf tanımı ...

 // AnalysisServiceImpl.java

 // ... importlar ve sınıf tanımı ...

	 @Override
	 public AnalysisResponseDto analyzeClass(String path) {
	     try {
	         // 1. Mevcut sistem classpath'ini al
	         String systemClassPath = System.getProperty("java.class.path");
	
	         // 2. Analiz edilecek projenin kök dizinini bul
	         JavaClass parsedForPath = MetricCalculator.parseClass(path);
	         String projectRoot = getClassPathRoot(path, parsedForPath.getClassName());
	
	         // 3. Mevcut classpath ile proje classpath'ini birleştir
	         String fullClassPath = systemClassPath + File.pathSeparator + projectRoot;
	
	         // 4. Bu birleşik path ile yeni, izole bir Repository oluştur
	         ClassPath cp = new ClassPath(fullClassPath);
	         // === EN ÖNEMLİ DÜZELTME: DOĞRU TİPİ KULLANMAK ===
	         org.apache.bcel.util.Repository localRepository = SyntheticRepository.getInstance(cp);
	
	         // 5. Analiz için sınıfı bu yeni repository üzerinden yükle
	         JavaClass javaClass = localRepository.loadClass(parsedForPath.getClassName());
	         MetricResultDto resultDto = MetricCalculator.calculateMetrics(javaClass, localRepository);
	
	         // 6. Sonuçları veritabanına kaydet
	         AnalysisEntity analysis = AnalysisEntity.builder()
	             .type("CLASS")
	             .targetPath(path)
	             .className(javaClass.getClassName())
	             .createdAt(LocalDateTime.now())
	             .build();
	
	         MetricResultEntity metricEntity = MetricResultEntity.builder()
	             .className(resultDto.getClassName())
	             .tcc(resultDto.getTcc())
	             .wmc(resultDto.getWmc())
	             .lcom(resultDto.getLcom())
	             .dit(resultDto.getDit())
	             .cbo(resultDto.getCbo())
	             .maxCyclo(resultDto.getMaxCyclo())
	             .avgCyclo(resultDto.getAvgCyclo())
	             .analysis(analysis)
	             .build();
	         analysis.setResults(Collections.singletonList(metricEntity));
	
	         AnalysisEntity saved = analysisRepository.save(analysis);
	
	         // 7. DTO'ya çevirip döndür
	         return new AnalysisResponseDto(
	             saved.getId(),
	             saved.getType(),
	             saved.getTargetPath(),
	             saved.getProjectName(),
	             saved.getCreatedAt(),
	             Collections.singletonList(resultDto) // doğrudan resultDto kullanılabilir
	         );
	
	     } catch (IOException | ClassNotFoundException e) {
	         throw new RuntimeException("Class analysis failed for path: " + path, e);
	     }
	 }
	
	 @Override
	 public AnalysisResponseDto analyzeJar(String jarFilePath) {
	     try {
	         File jarFile = new File(jarFilePath);
	         if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
	             throw new IllegalArgumentException("Verilen yol geçerli bir .jar dosyası değil: " + jarFilePath);
	         }

	         // 1. Mevcut sistem classpath'ini ve analiz edilecek JAR dosyasını birleştir
	         String systemClassPath = System.getProperty("java.class.path");
	         String fullClassPath = systemClassPath + File.pathSeparator + jarFilePath;

	         // 2. Bu birleşik path ile yeni, izole bir Repository oluştur
	         // BCEL, classpath'e eklenen JAR dosyasının içini otomatik olarak okuyabilir.
	         ClassPath cp = new ClassPath(fullClassPath);
	         org.apache.bcel.util.Repository localRepository = SyntheticRepository.getInstance(cp);

	         // 3. Analiz sonuçlarını tutacak ana Entity'i oluştur
	         AnalysisEntity analysis = AnalysisEntity.builder()
	             .type("JAR") // Analiz tipini JAR olarak belirtiyoruz
	             .targetPath(jarFilePath)
	             .projectName(jarFile.getName()) // Proje adı olarak JAR dosyasının adını kullanıyoruz
	             .createdAt(LocalDateTime.now())
	             .build();

	         List<MetricResultEntity> metricEntities = new ArrayList<>();

	         // 4. JAR dosyasının içini gezerek .class dosyalarını analiz et
	         try (JarFile jar = new JarFile(jarFile)) {
	             // JAR içindeki tüm girdileri (dosya/klasör) al
	             java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

	             while (entries.hasMoreElements()) {
	                 java.util.jar.JarEntry entry = entries.nextElement();

	                 // Eğer girdi bir klasör değilse ve .class ile bitiyorsa
	                 if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
	                     try {
	                         // Dosya yolunu (örn: com/example/MyClass.class) sınıf adına (örn: com.example.MyClass) çevir
	                         String className = entry.getName().replace('/', '.').replace(".class", "");

	                         // Sınıfı izole repository üzerinden yükle
	                         JavaClass javaClass = localRepository.loadClass(className);

	                         // Metrikleri hesapla
	                         MetricResultDto dto = MetricCalculator.calculateMetrics(javaClass, localRepository);

	                         // Sonucu veritabanı entity'sine dönüştür ve listeye ekle
	                         MetricResultEntity me = MetricResultEntity.builder()
	                             .className(dto.getClassName())
	                             .tcc(dto.getTcc())
	                             .wmc(dto.getWmc())
	                             .lcom(dto.getLcom())
	                             .dit(dto.getDit())
	                             .cbo(dto.getCbo())
	                             .maxCyclo(dto.getMaxCyclo())
	                             .avgCyclo(dto.getAvgCyclo())
	                             .analysis(analysis)
	                             .build();
	                         metricEntities.add(me);

	                     } catch (ClassNotFoundException e) {
	                         System.err.println("HATA: Sınıf bulunamadı veya yüklenemedi: " + entry.getName() + " - " + e.getMessage());
	                         // Analize devam et, bu sınıfı atla
	                     } catch (Exception e) {
	                         System.err.println("HATA: " + entry.getName() + " sınıfı işlenirken beklenmedik bir hata oluştu.");
	                         e.printStackTrace();
	                     }
	                 }
	             }
	         }

	         // 5. Sonuçları veritabanına kaydet
	         analysis.setResults(metricEntities);
	         AnalysisEntity saved = analysisRepository.save(analysis);

	         // 6. Sonuçları DTO'ya çevirip döndür
	         List<MetricResultDto> results = saved.getResults().stream()
	             .map(me -> new MetricResultDto(
	                 me.getId(),
	                 me.getClassName(),
	                 me.getTcc(),
	                 me.getWmc(),
	                 me.getLcom(),
	                 me.getDit(),
	                 me.getCbo(),
	                 me.getMaxCyclo(),
	                 me.getAvgCyclo()
	             ))
	             .collect(Collectors.toList());
	         
	         return new AnalysisResponseDto(
	             saved.getId(),
	             saved.getType(),
	             saved.getTargetPath(),
	             saved.getProjectName(),
	             saved.getCreatedAt(),
	             results
	         );
	         
	     } catch (IOException e) {
	         throw new RuntimeException("JAR analizi başarısız oldu: " + jarFilePath, e);
	     }
	 }
	 
	 
	 @Override
     public AnalysisResponseDto analyzeProject(String targetPath) {
         // 1. Mevcut sistem classpath'ini al
         String systemClassPath = System.getProperty("java.class.path");
         // 2. Proje yolu ile birleştir
         String fullClassPath = systemClassPath + File.pathSeparator + targetPath;
         // 3. Yeni, izole bir Repository oluştur
         ClassPath cp = new ClassPath(fullClassPath);
         // === EN ÖNEMLİ DÜZELTME: DOĞRU TİPİ KULLANMAK ===
         org.apache.bcel.util.Repository localRepository = SyntheticRepository.getInstance(cp);
         File folder = new File(targetPath);
         AnalysisEntity analysis = AnalysisEntity.builder().type("PROJECT").targetPath(targetPath).projectName(folder.getName()).createdAt(LocalDateTime.now()).build();
         List < MetricResultEntity > metricEntities = new ArrayList < > ();
         traverseAndAnalyze(folder, metricEntities, analysis, localRepository);
         analysis.setResults(metricEntities);
         AnalysisEntity saved = analysisRepository.save(analysis);
         List < MetricResultDto > results = saved.getResults().stream().map(me -> new MetricResultDto(me.getId(), me.getClassName(), me.getTcc(), me.getWmc(), me.getLcom(), me.getDit(), me.getCbo(), me.getMaxCyclo(), me.getAvgCyclo())).collect(Collectors.toList());
         return new AnalysisResponseDto(saved.getId(), saved.getType(), saved.getTargetPath(), saved.getProjectName(), saved.getCreatedAt(), results);
     }
	// Lütfen mevcut traverseAndAnalyze metodunuzu silip bunu yapıştırın.
	 private void traverseAndAnalyze(File file,
	                                 List<MetricResultEntity> metricEntities,
	                                 AnalysisEntity analysis,
	                                 org.apache.bcel.util.Repository repository) {

	     // 1. Adım: Hangi dosya veya klasörün işlendiğini yazdır
	     System.out.println(">>> İşleniyor: " + file.getAbsolutePath());

	     if (file.isDirectory()) {
	         System.out.println("    -> Bu bir KLASÖR. İçine giriliyor...");
	         File[] children = file.listFiles();
	         if (children == null) {
	             System.out.println("    -> UYARI: Klasörün içi okunamadı veya boş (listFiles() null döndü).");
	             return;
	         }
	         for (File child : children) {
	             traverseAndAnalyze(child, metricEntities, analysis, repository);
	         }
	     } else {
	         // 2. Adım: else bloğuna girildiğini ve koşul sonuçlarını yazdır
	         boolean isAFile = file.isFile();
	         boolean endsWithClass = file.getName().endsWith(".class");
	         System.out.println("    -> Bu bir dosya. Koşullar kontrol ediliyor...");
	         System.out.println("        - file.isFile()? : " + isAFile);
	         System.out.println("        - file.getName().endsWith(\".class\")? : " + endsWithClass);

	         if (isAFile && endsWithClass) {
	             // 3. Adım: En kritik nokta, try bloğuna girilip girilmediğini yazdır
	             System.out.println("    -> KOŞUL BAŞARILI! TRY BLOĞUNA GİRİLİYOR...");
	             try {
	                 String className = getClassNameFromFile(file, new File(analysis.getTargetPath()));
	                 System.out.println("        -> Sınıf adı bulundu: " + className);

	                 JavaClass javaClass = repository.loadClass(className);
	                 System.out.println("        -> Sınıf başarıyla yüklendi: " + javaClass.getClassName());

	                 MetricResultDto dto = MetricCalculator.calculateMetrics(javaClass, repository);
	                 System.out.println("        -> Metrikler hesaplandı: DIT=" + dto.getDit());

	                 MetricResultEntity me = MetricResultEntity.builder()
	                     .className(dto.getClassName())
	                     .tcc(dto.getTcc())
	                     .wmc(dto.getWmc())
	                     .lcom(dto.getLcom())
	                     .dit(dto.getDit())
	                     .cbo(dto.getCbo())
	                     .maxCyclo(dto.getMaxCyclo())
	                     .avgCyclo(dto.getAvgCyclo())	                     
	                     .analysis(analysis)
	                     .build();
	                 metricEntities.add(me);

	                 System.out.println("    -> BAŞARILI: Analiz tamamlandı ve listeye eklendi.");

	             } catch (Exception e) { // Genel Exception'ı yakalayalım
	                 // 4. Adım: try bloğu içinde bir hata olursa bunu yazdır
	                 System.err.println("    -> HATA: try bloğu içinde beklenmedik bir hata oluştu!");
	                 e.printStackTrace(); // Hatanın tüm detayını konsola yazdır
	             }
	         } else {
	             System.out.println("    -> KOŞUL BAŞARISIZ! Bu dosya atlanıyor.");
	         }
	     }
	     System.out.println("<<< Tamamlandı: " + file.getAbsolutePath());
	     System.out.println("-----------------------------------------------------");
	 }
	
	 // Yeni bir yardımcı metot daha: Dosya yolundan sınıf adını çıkaran
	 private String getClassNameFromFile(File classFile, File projectRoot) {
	     String path = classFile.getAbsolutePath();
	     String rootPath = projectRoot.getAbsolutePath();
	
	     String relativePath = path.substring(rootPath.length() + 1); // +1 for the separator
	     return relativePath.replace(File.separatorChar, '.').replace(".class", "");
	 }
	
	@Override
    public List<AnalysisSummaryDto> getAllAnalyses() {
        return analysisRepository.findAll()
            .stream()
            .map(entity -> new AnalysisSummaryDto(
                entity.getId(),
                entity.getType(),
                entity.getTargetPath(),
                entity.getProjectName(),
                entity.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public AnalysisResponseDto getAnalysisById(UUID id) {
        AnalysisEntity entity = analysisRepository.findById(id)
            .orElseThrow(() -> 
                new RuntimeException("Analysis not found with id: " + id)
            );

        List<MetricResultDto> results = entity.getResults()
            .stream()
            .map(res -> new MetricResultDto(
                res.getId(),
                res.getClassName(),
                res.getTcc(),
                res.getWmc(),
                res.getLcom(),
                res.getDit(),
                res.getCbo(),
                res.getMaxCyclo(),
                res.getAvgCyclo()
            ))
            .collect(Collectors.toList());

        return new AnalysisResponseDto(
            entity.getId(),
            entity.getType(),
            entity.getTargetPath(),
            entity.getProjectName(),
            entity.getCreatedAt(),
            results
        );
    }
    
    @Override
    public VisualizeResponseDTO visualize(VisualizeRequest req) {
        // 1) Varolan analiz verisini alın
    	AnalysisResponseDto analysis = getAnalysisById(req.getId());

        // 2) Her bir sınıf için sadece seçilen metrikleri map’leyin
        List<VisualizeResultDTO> filtered = analysis.getResults().stream()
            .map(r -> {
                // Dinamik bir map oluştur
                Map<String, Number> map = new HashMap<>();
                for (String metric : req.getMetrics()) {
                    switch (metric.toLowerCase()) {
                        case "tcc":  map.put("tcc",  r.getTcc());  break;
                        case "wmc":  map.put("wmc",  r.getWmc());  break;
                        case "lcom": map.put("lcom", r.getLcom()); break;
                        case "dit":  map.put("dit",  r.getDit());  break;
                        case "cbo":  map.put("cbo",  r.getCbo());  break;
                        case "maxcyclo":	map.put("maxcyclo", r.getMaxCyclo()); break;
                        case "avgcyclo":	map.put("avgcyclo", r.getAvgCyclo()); break;
                        
                    }
                }
                VisualizeResultDTO vr = new VisualizeResultDTO();
                vr.setClassName(r.getClassName());
                vr.setMetrics(map);
                return vr;
            })
            .collect(Collectors.toList());

        // 3) VisualizeResponseDTO’yu doldurun
        VisualizeResponseDTO resp = new VisualizeResponseDTO();
        resp.setId(analysis.getId());
        resp.setType(analysis.getType());
        resp.setTargetPath(analysis.getTargetPath());
        resp.setProjectName(analysis.getProjectName());
        resp.setCreatedAt(analysis.getCreatedAt());
        resp.setResults(filtered);

        return resp;
    }
    
 // Bu metodu AnalysisServiceImpl sınıfınızın içine herhangi bir yere ekleyin.
    /**
     * Verilen bir .class dosyasının tam yolundan ve sınıf adından yola çıkarak,
     * projenin classpath kök dizinini bulur.
     * Örn: path="C:\proj\bin\com\test\MyClass.class", className="com.test.MyClass"
     * Dönen Değer: "C:\proj\bin"
     */
    private String getClassPathRoot(String path, String className) {
        String packagePath = className.replace('.', File.separatorChar);
        String fullClassPath = packagePath + ".class";
        
        if (path.endsWith(fullClassPath)) {
            return path.substring(0, path.length() - fullClassPath.length());
        }
        return new File(path).getParent(); // Fallback
    }

}
