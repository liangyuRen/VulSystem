# Language Detection - Implementation Guide

## Files to Modify: 3

---

## FILE 1: ProjectService.java (Interface)
Path: backend/src/main/java/com/nju/backend/service/project/ProjectService.java

ADD THIS METHOD TO INTERFACE:

    /**
     * Detect project language based on file structure
     */
    String detectLanguageFromPath(String filePath);

---

## FILE 2: ProjectServiceImpl.java (Implementation)
Path: backend/src/main/java/com/nju/backend/service/project/Impl/ProjectServiceImpl.java

ADD THIS METHOD IMPLEMENTATION:

    @Override
    public String detectLanguageFromPath(String filePath) {
        try {
            String detectedLanguage = projectUtil.detectProjectType(filePath);
            System.out.println("Detected language: " + detectedLanguage);
            
            // Return detected language or 'unknown' if detection fails
            return detectedLanguage \!= null ? detectedLanguage : "unknown";
        } catch (IOException e) {
            System.err.println("Language detection failed: " + e.getMessage());
            return "unknown";
        }
    }

---

## FILE 3: ProjectController.java (Integration)
Path: backend/src/main/java/com/nju/backend/controller/ProjectController.java

LOCATE: uploadProject method, around line 81-82

FIND:
    String projectLanguage = (language \!= null && \!language.isEmpty()) ? language : "java";

REPLACE WITH:
    String projectLanguage = language;
    
    // If language not provided, auto-detect from project structure
    if (projectLanguage == null || projectLanguage.isEmpty()) {
        System.out.println("Language not provided, auto-detecting from file structure...");
        projectLanguage = projectService.detectLanguageFromPath(filePath);
        System.out.println("Language detection result: " + projectLanguage);
        
        // Fallback to unknown if detection fails
        if (projectLanguage == null || projectLanguage.isEmpty()) {
            projectLanguage = "unknown";
        }
    }

---

## TESTING STEPS

1. Rebuild the project:
   mvn clean install

2. Run the application:
   mvn spring-boot:run

3. Test with go-ethereum project (no language parameter):
   curl -X POST "http://localhost:8081/project/uploadProject"      -F "file=@go-ethereum.zip"      -F "name=ethereum-test"      -F "description=Ethereum blockchain"      -F "companyId=1"

4. Check the detected language:
   curl "http://localhost:8081/project/list?companyId=1&page=1&size=20"

5. Expected: language should NOT be "java" if detection works

---

## VERIFICATION CHECKLIST

- [ ] ProjectService interface updated
- [ ] ProjectServiceImpl method implemented
- [ ] ProjectController code replaced
- [ ] Project compiles without errors
- [ ] Backend starts successfully
- [ ] Upload test with go-ethereum succeeds
- [ ] Detected language is NOT "java"
- [ ] Test with explicit language parameter still works
- [ ] Java projects still detected as "java"


