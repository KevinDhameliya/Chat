# Chat 💬

A modern Android Chat Application built using **Kotlin** and Android's traditional **XML View System**, following **Clean Architecture** and the **MVVM** architectural pattern.

This project aims to provide a scalable, maintainable, and production-ready chat application while following Android development best practices.

---

## 🚀 Features

- User Authentication
- One-to-One Chat
- Real-time Messaging
- Online/Offline Status
- Typing Indicator
- Read Receipts
- Message Delivery Status
- Image & File Sharing
- Push Notifications
- Search Conversations
- User Profile Management
- Dark Mode Support

---

## 🛠️ Tech Stack

- Kotlin
- Android XML
- MVVM Architecture
- Clean Architecture
- Repository Pattern
- Android Jetpack Components
- LiveData
- ViewModel
- Coroutines
- Hilt (Dependency Injection)
- Room Database
- Retrofit
- OkHttp
- Firebase Authentication
- Firebase Cloud Messaging (FCM)
- Navigation Component
- Material Design Components

---

## 📂 Project Structure

```
com.kevin.chat
│
├── data
│   ├── local
│   ├── remote
│   ├── repository
│   └── model
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── presentation
│   ├── auth
│   ├── home
│   ├── chat
│   ├── profile
│   ├── settings
│   ├── adapters
│   ├── fragments
│   ├── activities
│   └── viewmodel
│
├── di
│
└── utils
```

---

## 🏗️ Architecture

This project follows **Clean Architecture** with the **MVVM** design pattern.

```
UI (Activity / Fragment)
          │
          ▼
      ViewModel
          │
          ▼
       Use Cases
          │
          ▼
      Repository
          │
    ┌─────┴─────┐
    ▼           ▼
 Local       Remote
(Room DB)   Firebase/API
```

---

## 📱 Planned Screens

- Splash Screen
- Login
- Register
- Home
- Chat List
- Chat Screen
- User Profile
- Settings

---

## 🎯 Project Goals

- Learn modern Android development using Kotlin.
- Build a scalable real-time chat application.
- Follow Clean Architecture and MVVM.
- Write clean, maintainable, and testable code.
- Apply Android development best practices.

---

## 📋 Requirements

- Android Studio (Latest Stable Version)
- Kotlin
- Android SDK 24+
- JDK 21

---

## ⚙️ Installation

Clone the repository:

```bash
git clone https://github.com/KevinDhameliya/Chat.git
```

Open the project in Android Studio and allow Gradle to sync.

---

## 📌 Future Improvements

- Group Chat
- Voice Messages
- Video Calling
- Message Reactions
- Chat Backup & Restore
- End-to-End Encryption
- Message Search
- Pinned Chats
- Archived Chats

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push the branch.
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**Kevin Dhameliya**

GitHub: https://github.com/KevinDhameliya