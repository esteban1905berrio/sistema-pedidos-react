# SAP JCo Library Directory

This directory contains the SAP Java Connector (JCo) libraries required for RFC communication with SAP systems.

## Required Files

You must manually download and place the following files in this directory:

### Platform-Specific Libraries

**All Platforms:**
- `sapjco3.jar` - Java library (platform-independent)

**Linux:**
- `libsapjco3.so` - Native library for Linux (x86_64 or aarch64)

**macOS:**
- `libsapjco3.jnilib` - Native library for macOS (Intel or Apple Silicon)

**Windows:**
- `sapjco3.dll` - Native library for Windows (x64)

## Download Instructions

### 1. Access SAP Support Portal

1. Go to https://support.sap.com/en/product/connectors/jco.html
2. Click "Download" button
3. Log in with your S-user credentials (SAP Service Marketplace account)

**Note:** You need a valid S-user account to download SAP JCo. Contact your SAP Basis team if you don't have access.

### 2. Select Correct Version

- **Recommended Version**: SAP JCo 3.1.9 or later
- **Java Compatibility**: Java 11+ (Java 17+ recommended)
- **Platform**: Match your development/deployment platform

### 3. Download Files

Download the appropriate package for your platform:

- **Linux x86_64**: `sapjco3-linuxx86_64-3.1.9.tgz`
- **Linux aarch64**: `sapjco3-linuxaarch64-3.1.9.tgz`
- **macOS**: `sapjco3-darwinintel64-3.1.9.tgz` or `sapjco3-darwinarm64-3.1.9.tgz`
- **Windows x64**: `sapjco3-ntamd64-3.1.9.zip`

### 4. Extract and Copy

Extract the downloaded archive and copy files to this directory:

```bash
# Linux/macOS
tar -xzf sapjco3-*.tgz
cp sapjco3.jar /path/to/java-mcp-server/lib/
cp libsapjco3.so /path/to/java-mcp-server/lib/  # or libsapjco3.jnilib on macOS

# Windows (PowerShell)
Expand-Archive sapjco3-*.zip
Copy-Item sapjco3.jar .\java-mcp-server\lib\
Copy-Item sapjco3.dll .\java-mcp-server\lib\
```

### 5. Verify Files

After copying, your `lib/` directory should look like:

```
lib/
├── README.md (this file)
├── sapjco3.jar
└── libsapjco3.so (or .jnilib, .dll)
```

## Licensing

**IMPORTANT:** SAP JCo libraries are **not open source** and cannot be redistributed.

- JCo libraries are subject to SAP licensing terms
- Do not commit these files to public repositories
- Each developer/deployment must download from SAP Support Portal
- `.gitignore` is configured to exclude these files

## Verification

### Verify JAR File

```bash
jar tf lib/sapjco3.jar | head -n 5
```

Expected output should show SAP connector classes:
```
com/sap/conn/jco/
com/sap/conn/jco/JCo.class
com/sap/conn/jco/JCoDestination.class
...
```

### Verify Native Library

**Linux/macOS:**
```bash
file lib/libsapjco3.so
```

Expected output:
```
lib/libsapjco3.so: ELF 64-bit LSB shared object, x86-64, version 1 (SYSV), dynamically linked, stripped
```

**Windows (PowerShell):**
```powershell
Get-Item lib\sapjco3.dll | Format-List
```

## Troubleshooting

### Problem: JCo Not Found

**Error:**
```
java.lang.NoClassDefFoundError: com/sap/conn/jco/JCoException
```

**Solution:** Ensure `sapjco3.jar` is in `lib/` directory and included in Maven dependencies.

---

### Problem: Native Library Not Found

**Error:**
```
java.lang.UnsatisfiedLinkError: no sapjco3 in java.library.path
```

**Solution:**

1. Verify native library is in `lib/` directory
2. Set library path when running:

```bash
# Linux/macOS
export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar

# Windows
set PATH=.\lib;%PATH%
java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar
```

---

### Problem: Platform Mismatch

**Error:**
```
java.lang.UnsatisfiedLinkError: ... wrong ELF class: ELFCLASS32
```

**Solution:** Download the correct version for your platform architecture (x86_64 vs aarch64, Intel vs ARM).

---

### Problem: Missing libc Dependencies (Linux)

**Error:**
```
error while loading shared libraries: libc.so.6
```

**Solution (Docker Alpine):**
```dockerfile
RUN apk add --no-cache libc6-compat
```

**Solution (Ubuntu/Debian):**
```bash
sudo apt-get install libc6
```

## Multi-Platform Docker Builds

For multi-platform Docker builds, organize native libraries by architecture:

```
lib/
├── sapjco3.jar
├── linux/
│   ├── amd64/
│   │   └── libsapjco3.so
│   └── arm64/
│       └── libsapjco3.so
├── darwin/
│   ├── amd64/
│   │   └── libsapjco3.jnilib
│   └── arm64/
│       └── libsapjco3.jnilib
└── windows/
    └── amd64/
        └── sapjco3.dll
```

Then use conditional COPY in Dockerfile:

```dockerfile
ARG TARGETPLATFORM
COPY lib/${TARGETPLATFORM}/libsapjco3.* /usr/lib/
```

## References

- [SAP JCo Documentation](https://support.sap.com/en/product/connectors/jco.html)
- [SAP JCo Release Notes](https://launchpad.support.sap.com/#/notes/1025361)
- [SAP JCo Java API](https://javadoc.io/doc/com.sap.cloud.sjb/jco/latest/index.html)

## Support

For licensing questions or download access issues, contact:
- **SAP Basis Team** (internal)
- **SAP Support Portal** (https://support.sap.com)

---

**Last Updated:** 2025-11-07
**SAP JCo Version:** 3.1.9
**Required for:** SAP MCP Server Java POC
