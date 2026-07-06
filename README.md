# Chat 💬

A modern Android Chat Application built using **Kotlin** and **Jetpack Compose** following **Clean Architecture** and **MVVM** principles.

The goal of this project is to create a scalable, maintainable, and production-ready chat application while following Android development best practices.

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
- Dark Mode Support

---

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Clean Architecture
- Repository Pattern
- Coroutines
- StateFlow
- Hilt (Dependency Injection)
- Room Database
- Retrofit
- OkHttp
- Firebase Authentication
- Firebase Cloud Messaging (FCM)
- Navigation Compose
- Material 3

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
│   ├── authentication
│   ├── chat
│   ├── home
│   ├── profile
│   ├── navigation
│   └── components
│
├── di
│
└── utils
```

---

## 📱 Architecture

This project follows **Clean Architecture** with the **MVVM** design pattern.

```
UI (Jetpack Compose)
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
 ┌──────┴──────┐
 ▼             ▼
Local       Remote
(Room)     (API/Firebase)
```

---

## 📸 Screens (Planned)

- Splash Screen
- Login
- Register
- Home
- Chat List
- Chat Screen
- Profile
- Settings

---

## 🎯 Project Goals

- Learn modern Android development.
- Follow Android best practices.
- Build a scalable chat application.
- Write clean and maintainable code.
- Practice Clean Architecture and MVVM.

---

## 📋 Requirements

- Android Studio Narwhal or newer
- Kotlin 2.x
- Android SDK 24+
- JDK 21

---

## ⚙️ Installation

```bash
git clone https://github.com/KevinDhameliya/Chat.git
```

Open the project in Android Studio and let Gradle sync complete.

---

## 🤝 Contributing

Contributions are welcome!

Feel free to fork the repository, create a feature branch, and submit a pull request.

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**Kevin Dhameliya**

GitHub: https://github.com/KevinDhameliya
