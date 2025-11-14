# Language Detection Interface Testing Plan

**Date**: 2025-11-13  
**Objective**: Test the automatic project language detection feature  
**Test Target**: POST /project/uploadProject without specifying language parameter

---

## Current Implementation Status

### Backend Language Detection Logic (ProjectUtil.java)

The system has a `detectProjectType()` method that analyzes project files by checking file extensions and specific markers:

#### Supported Languages

1. **Java** - Detected by:
   - pom.xml (Maven build file)
   - build.gradle (Gradle build file)
   - .java files

2. **C/C++** - Detected by:
   - Makefile
   - CMakeLists.txt
   - .c files
   - .h files

3. **Unknown** - When no recognized patterns found

#### Detection Logic (Line 491-625)

```java
public static Map<String, Double> calcLanguagePercentByFileSize(String projectPath) {
    // Step 1: Map file extensions to languages
    Map<String, String> EXT_LANG_MAP = new HashMap<>();
    EXT_LANG_MAP.put("java", "Java");
    EXT_LANG_MAP.put("c", "C");
    EXT_LANG_MAP.put("cpp", "C++");
    EXT_LANG_MAP.put("h", "C/C++Header");
    EXT_LANG_MAP.put("py", "Python");
    EXT_LANG_MAP.put("js", "JavaScript");
    EXT_LANG_MAP.put("ts", "TypeScript");

    // Step 2: Count file sizes by language
    Map<String, Double> percent = new HashMap<>();
    // Calculate percentage of each language
    return percent;
}

public String detectProjectType(String projectPath) {
    // Checks for:
    // 1. pom.xml, build.gradle -> Java
    // 2. Makefile, CMakeLists.txt, .c, .h -> C/C++
    // 3. Otherwise -> unknown
}
```

---

## Current Upload Flow

### 1. uploadProject Endpoint (Line 56-108 in ProjectController.java)

```java
@PostMapping("/uploadProject")
public RespBean uploadProject(
    @RequestParam("file") MultipartFile file,
    @RequestParam("name") String name,
    @RequestParam("description") String description,
    @RequestParam(value = "language", required = false) String language,  // Optional!
    @RequestParam(value = "risk_threshold", required = false) Integer risk_threshold,
    @RequestParam("companyId") int companyId)
```

**Current Behavior (Line 82)**:
```java
// If language not provided, defaults to "java"
String projectLanguage = (language != null && !language.isEmpty()) ? language : "java";
```

**ISSUE**: The system defaults to "java" instead of detecting the actual language!

---

## Proposed Solution

Modify the upload flow to automatically detect language:

1. **Upload file** → unzip to temporary directory
2. **Analyze structure** → run `detectProjectType()`
3. **Create project** → with detected language

---

## Test Case: New Project in Database

### Test Project Details
- **Name**: (specified by user in database)
- **Upload Status**: Already uploaded (ZIP file in D:\kuling\upload\)
- **Current Language Setting**: Not specified during upload
- **Expected Detection**: Should detect based on project structure

---

## Testing Approach

### Pre-Test: Understand Current Project Structure

1. Check database for the new project
2. Locate the uploaded files in `D:\kuling\upload\`
3. Examine the project structure

### Test Steps

**Step 1**: Upload the project WITHOUT language parameter

```bash
curl -X POST "http://localhost:8081/project/uploadProject" \
  -F "file=@path/to/project.zip" \
  -F "name=TestProject" \
  -F "description=Auto-detect language test" \
  -F "companyId=1"
```

Expected Behavior:
- System should automatically detect language
- Not default to "java"

**Step 2**: Verify detected language

```bash
curl "http://localhost:8081/project/info?projectid=<ID>"
```

Check response for language field.

**Step 3**: Compare with manual upload

Upload the same project with explicit language:

```bash
curl -X POST "http://localhost:8081/project/uploadProject" \
  -F "file=@path/to/project.zip" \
  -F "name=TestProject2" \
  -F "description=Explicit language test" \
  -F "language=java" \
  -F "companyId=1"
```

---

## Required Code Changes

### Modification 1: ProjectController.java

**Current Code (Line 81-82)**:
```java
String projectLanguage = (language != null && !language.isEmpty()) ? language : "java";
```

**Proposed Code**:
```java
String projectLanguage = language;

// If language not provided, auto-detect from uploaded file
if (projectLanguage == null || projectLanguage.isEmpty()) {
    System.out.println("Language not provided, auto-detecting...");
    projectLanguage = projectService.detectLanguageFromPath(filePath);
    System.out.println("Auto-detected language: " + projectLanguage);
}
```

### Modification 2: ProjectService Interface

Add new method:

```java
String detectLanguageFromPath(String filePath);
```

### Modification 3: ProjectServiceImpl.java

Implement the detection method:

```java
@Override
public String detectLanguageFromPath(String filePath) {
    try {
        return projectUtil.detectProjectType(filePath);
    } catch (IOException e) {
        System.err.println("Language detection failed: " + e.getMessage());
        // Fallback to default if detection fails
        return "unknown";
    }
}
```

---

## Test Data Information

### New Project Details

Please provide:
1. **Project Name**: ?
2. **Upload Location**: D:\kuling\upload\{UUID}\?
3. **Expected Language**: ?

Once provided, I can:
1. Verify the actual file structure
2. Run language detection test
3. Confirm detection accuracy
4. Recommend necessary code modifications

---

## Detection Accuracy Matrix

| File Pattern | Detection | Confidence |
|---|---|---|
| pom.xml | java | High |
| build.gradle | java | High |
| .java files | java | High |
| Makefile | c | High |
| CMakeLists.txt | c | High |
| .c, .c++, .h files | c | High |
| No match | unknown | N/A |

---

## Next Steps

1. Provide the new project details
2. Verify file structure in upload directory
3. Run detection tests
4. Implement code modifications
5. Regression test with existing projects

---

## Notes

- Current system defaults to "java" (incorrect behavior)
- Language detection method already exists but is not used
- Easy fix: wire up existing detection logic to upload endpoint
- No breaking changes to API - language parameter remains optional

