# 📱 Mini CRM App + Flutter Viewer

A **Mini CRM (Customer Relationship Management)** system built with **Jetpack Compose (Kotlin)** for Android, with a companion **Flutter Viewer App** for read-only cross-platform access.  

This project demonstrates an **offline-first architecture**, syncing data between a **local Room DB** and **Firebase Firestore**, with background sync powered by WorkManager.

---

## ✨ Features
- 🔑 **Firebase Authentication** (Email/Password)
- 👥 **Customer Management** (Add, Edit, Delete, View Details)
- 🛒 **Order Management** (Per Customer)
- 🔄 **Offline-first architecture** using Room DB
- 📡 **Import Random Company API** for generating demo data
- 🎨 **Modern Material 3 UI** with Jetpack Compose
- 👀 **Flutter Viewer App** for browsing customers/orders on multiple platforms

---

## 📹 Demo
👉 [Watch Demo Video](https://your-demo-link.com)  
👉 [Download APK](https://your-apk-link.com)  

---

## 🏗️ Tech Stack

### Android Mini CRM (Main App)
- **Language/UI**: Kotlin, Jetpack Compose, Material 3
- **Architecture**: MVVM + Repository
- **Local DB**: Room + Kotlin Flow
- **Remote**: Firebase Auth, Firestore
- **Networking**: Retrofit
- **Prefs**: DataStore

---

## 🚀 Installation

### 🔹 Android Mini CRM App
1. Clone the repository:
   ```bash
   git clone https://github.com/Sovan22/MiniCRMApp.git
   cd android_crm/MiniCRM
   ```
2. Open in Android Studio (Giraffe or newer).

3. Add your google-services.json to /app.

4. Run the app:
   ```bash
    ./gradlew installDebug
   ```
5. Or build a release APK:
   ```bash
    ./gradlew assembleRelease
   ```
