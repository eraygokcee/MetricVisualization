# 3D Visualization of Object-Oriented Software Metrics

This project analyzes the structural quality of object-oriented software systems and visualizes software metrics using a **3D city metaphor**.  
Instead of interpreting metrics as raw numerical values, the system enables developers to **intuitively explore architectural complexity and design issues** through interactive visualization.

This project was developed as a **senior graduation project** at Yıldız Technical University, Computer Engineering Department.

---

## 🎯 Project Overview

- Analyzes **compiled Java (.class / .jar) files** using bytecode-level analysis  
- Calculates widely used **object-oriented software metrics (CK Metrics)**  
- Stores analysis results to allow **historical comparison**  
- Visualizes selected metrics in a **3D environment using Unity**, where:
  - Each class is represented as a building
  - Metric values are mapped to **height, width, and color**

This approach helps identify:
- Highly complex classes  
- Tight coupling and low cohesion  
- Potential architectural bottlenecks in large-scale systems  

---

## 🧠 Key Concepts & Metrics

The system calculates and visualizes metrics such as:

- **WMC** – Weighted Methods per Class  
- **DIT** – Depth of Inheritance Tree  
- **CBO** – Coupling Between Object Classes  
- **LCOM / TCC** – Cohesion-related metrics  
- **Cyclomatic Complexity** (AVG / MAX)  

Metrics are selected dynamically and mapped to visual properties, enabling flexible analysis scenarios.

---

## 🏗️ System Architecture

The project follows a **multi-layered architecture** consisting of three main components:

### 🔹 Backend (Analysis Layer)
- **Java & Spring Boot**
- Bytecode analysis using **Apache BCEL**
- Metric calculation algorithms implemented at service level
- REST APIs exposing analysis results in **JSON format**
- Metric results stored in a relational database

### 🔹 Frontend (User Interface)
- **React.js**
- Handles:
  - Project and file selection
  - Triggering metric analysis
  - Viewing previous analysis records
- Acts as a bridge between backend services and visualization layer

### 🔹 Visualization Layer
- **Unity (C#)**
- Implements the **software city metaphor**
- Dynamically generates 3D structures based on metric data
- Interactive features:
  - Hover to inspect class-level metrics
  - Click to view detailed information
  - Camera navigation for large-scale systems

---

## 🏙️ Visualization Logic

- Each **class is represented as a single building**
- Metric mappings:
  - Height → complexity-related metrics
  - Width → size or dependency metrics
  - Color intensity → cohesion or coupling metrics
- Metric-to-visual mapping is configurable by the user

This enables a **holistic, top-down view** of software architecture, similar to observing a real city layout.

---

## 🧪 Validation & Results

The system was tested on projects of varying sizes:
- Small-scale projects (10–20 classes)
- Medium-scale projects (100+ classes)

Results demonstrated:
- Consistent and accurate metric calculations
- Acceptable analysis times for medium-sized systems
- High correlation between expected theoretical values and computed metrics

---

## 📌 Notes

- This repository focuses on **core implementation and visualization logic**
- Full Unity project files and build artifacts are intentionally excluded
- The project emphasizes **architecture, software metrics, and visualization design**

---

## 🛠️ Technologies Used

- **Java, Spring Boot**
- **Apache BCEL**
- **React.js**
- **Unity (C#)**
- **REST APIs, JSON**
- **Relational Database**

---

## 👤 Author

**Eray Gökçe**  
Computer Engineering  
Yıldız Technical University
