# PeakForm

**PeakForm** is an open-source Android app for tracking your weight, calorie intake, and progress toward fitness goals. Designed for lifters, athletes, and anyone who wants to manage their body composition in a data-driven, privacy-respecting way.

![PeakForm Banner](docs/banner.png) 

---

## Features

- 📈 **Weight & Calorie Logging**: Fast entry for daily bodyweight and calories eaten.
- 🔥 **Dynamic Maintenance Calculator**: Get a personalized estimate of your maintenance calories based on your own logged data, not generic formulas.
- 🎯 **Goal Tracking**: Set custom goals—cut, bulk, or maintain—by date, rate, or target weight.
- 🗓️ **Trends & Analytics**: Visualize progress with trend lines, goal lines, and month-by-month breakdowns (more charting options coming soon).
- 🔁 **Health Connect Sync**: Import/export weight data with Health Connect.
- ⚙️ **Customizable Settings**: Choose kg/lbs and configure your tracking preferences.
- 🔒 **100% Local, 100% Private**: All data stays on your device unless you export it. I really don't want your data.

---

## Screenshots

<!-- TODO: Add screenshots here -->
<p float="left">
  <img src="docs/screenshot_log.png" width="200">
  <img src="docs/screenshot_trends.png" width="200">
  <img src="docs/screenshot_goal.png" width="200">
</p>

---

## Getting Started

### 📱 **Build Locally**

1. **Clone this repo:**
    ```bash
    git clone https://github.com/YOURUSERNAME/peakform.git
    cd peakform
    ```
2. **Open in Android Studio.**
3. **Build & run on a device or emulator.**

> Minimum SDK: 26 (Android 8.0+)

### 🛠 **Tech Stack**

- **Kotlin** + Jetpack Compose
- **Room** (local database)
- **MPAndroidChart** (charting)
- **Google Health Connect** (sync)
- **Material 3** (theming)

---

## Contributing

Pull requests and issues are welcome!  
Check out the [contributing guidelines](CONTRIBUTING.md) (coming soon) or open an issue to suggest a feature or report a bug.

---

## Roadmap

- [ ] Month-by-month chart navigation
- [ ] Theming & visual polish
- [ ] Better localisation support. KG vs LBS, date formats etc.

---

## License

[MIT](LICENSE)

---

*PeakForm is built on days off, late nights, and a touch of healthy obsession for body composition and data.*
