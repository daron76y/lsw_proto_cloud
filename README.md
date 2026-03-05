# LSW Proto Cloud - Multi-Module Game System

A cloud-based RPG system built with Spring Boot and Java 23.

## Table of Contents
- [Project Overview](#project-overview)
- [Project Structure](#project-structure)
- [CI/CD Pipeline](#cicd-pipeline)
- [Team Contributions](#team-contributions)

## Project Overview

This project is a multi-module Maven application with four main components:

```
lsw_proto_cloud/
├── core/          # Shared domain models and utilities
├── battle/        # Battle mechanics engine
├── pve/           # PvE (player vs environment) campaigns
└── pvp/           # PvP (player vs player) matches
```

**Tech Stack:**
- Java 23
- Spring Boot 3.3.1
- Maven (multi-module build)
- JUnit (testing)
- Docker (containerization)

## Project Structure

```
lsw_proto_cloud/
├── .github/
│   └── workflows/
│       └── ci.yml              # CI/CD Pipeline Configuration
├── battle/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── core/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── pve/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── pvp/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── pom.xml                     # Parent POM
├── README.md
└── CI-CD.md                   # Pipeline Documentation
```

## CI/CD Pipeline

We'll be using **GitHub Actions** for continuous integration and deployment automation.

### What the Pipeline Does

The automated CI/CD pipeline runs on every push and pull request to the `main` and `develop` branches:

**Pipeline Stages:**
1. **Build & Test** - Compiles all Maven modules and runs JUnit tests
2. **Code Quality** - Performs Maven verify checks
3. **Docker Build** - Creates container images for deployment (main branch only)

**Configuration File:** [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

**Detailed Documentation:** See [CI-CD.md](CI-CD.md) for complete pipeline documentation.

### Pipeline Architecture

```
GitHub Push/PR
      │
      ▼
┌─────────────────┐
│ Build & Test    │──▶ Maven clean install
│                 │──▶ Run all JUnit tests
│                 │──▶ Generate test reports
└─────────────────┘
      │
      ├──▶ ┌─────────────────┐
      │    │ Code Quality    │──▶ Maven verify
      │    └─────────────────┘
      │
      └──▶ ┌─────────────────┐
           │ Docker Build    │──▶ Build images (main only)
           │ (Matrix: 4x)    │    • battle
           └─────────────────┘    • pve
                                  • pvp
                                  • core
```

### Viewing Pipeline Results

- **Actions Tab:** [View Workflow Runs](https://github.com/GITHUB_USERNAME/lsw_proto_cloud/actions)
- **Build Status Badge:** Displayed at the top of this README
- **Test Reports:** Available in the Actions UI after each run
- **Artifacts:** Build artifacts (JARs) are uploaded and available for 7 days

### Triggering the Pipeline

**Automatic Triggers:**
- Push to `main` or `develop` branches
- Pull requests targeting `main` or `develop`

**Manual Trigger:**
1. Go to the Actions tab
2. Select "CI/CD Pipeline"
3. Click "Run workflow"
4. Choose your branch and run


**Note**: Replace `GITHUB_USERNAME` with your actual GitHub username in the workflow file and links above.
