Prayer Times Library
A Kotlin library for calculating Islamic prayer times based on latitude, longitude, and calculation methods. This library supports multiple calculation methods (e.g., Umm al-Qura, Muslim World League) and provides accurate prayer times for any location.

Features
🕌 Multiple Calculation Methods: Supports popular calculation methods like Umm al-Qura, Muslim World League, University of Islamic Sciences Karachi, and more.

🌍 Global Coverage: Accurate prayer times for any location worldwide.

🕒 Customizable: Easily adjust Fajr, Isha, and other prayer times using custom angles or offsets.

📅 Date-Based Calculations: Get prayer times for any date.

🚀 Easy Integration: Seamlessly integrate into Android projects with minimal setup.

Installation
Add the dependency to your app’s build.gradle file:

gradle

**repositories {
    maven { url 'https://jitpack.io' }
}


dependencies {
    implementation("com.github.waqaramjad0012:PrayerTimesLibrary:v1.0.2")
}**




Usage
1. Initialize the Library
kotlin
Copy
val prayTime = PrayTime()
2. Set Calculation Method
Choose a calculation method from the CalculationMethod enum:

kotlin
Copy
prayTime.setCalcMethod(CalculationMethod.UMM_AL_QURAH_MAKKAH)
Supported methods include:

UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI

UMM_AL_QURAH_MAKKAH

MUSLIM_WORLD_LEAGUE

EGYPTIAN_GENERAL_AUTHORITY

NORTH_AMERICA

DUBAI

KUWAIT

QATAR

SINGAPORE

3. Get Prayer Times
Calculate prayer times for a specific date and location:

kotlin
Copy
val latitude = -33.8688 // Sydney, Australia
val longitude = 151.2093
val timeZone = 10.0 // UTC+10 for Sydney

val prayerTimes = prayTime.getDatePrayerTimes(2023, 10, 15, latitude, longitude, timeZone)
4. Display Prayer Times
kotlin
Copy
prayerTimes.forEachIndexed { index, time ->
    println("${prayTime.timeNames[index]}: $time")
}
Output Example
Copy
Fajr: 05:12
Sunrise: 06:34
Dhuhr: 12:05
Asr: 15:22
Sunset: 17:36
Maghrib: 17:36
Isha: 19:00
Supported Calculation Methods
Method	Fajr Angle	Isha Angle	Asr Method	Usage
University of Islamic Sciences, Karachi	18°	18°	Hanafi (2x shadow)	South Asia, Middle East
Umm al-Qura, Makkah	18.5°	90 mins after Maghrib	Standard	Saudi Arabia
Muslim World League (MWL)	18°	17°	Standard	Europe, North America, Asia
Egyptian General Authority of Survey	19.5°	17.5°	Standard	Egypt, Africa
North America (ISNA)	15°	15°	Standard	United States, Canada
Dubai	18.2°	18.2°	Standard	UAE
Kuwait	18°	17.5°	Standard	Kuwait
Qatar	18°	18°	Standard	Qatar
Singapore	20°	18°	Standard	Singapore
Customization
Adjust High Latitude Methods
For regions close to the poles (e.g., Scandinavia, Alaska), use one of the following high-latitude adjustment methods:

kotlin
Copy
prayTime.setAdjustHighLats(AdjustHighLats.ANGLE_BASED)
Supported methods:

NONE: No adjustment.

MIDNIGHT: Middle of the night.

ONESEVENTH: 1/7th of the night.

ANGLE_BASED: Angle/60th of the night.

Contributing
Contributions are welcome! If you’d like to contribute, please follow these steps:

Fork the repository.

Create a new branch (git checkout -b feature/YourFeature).

Commit your changes (git commit -m 'Add some feature').

Push to the branch (git push origin feature/YourFeature).

Open a pull request.

License
This project is licensed under the MIT License. See the LICENSE file for details.

Acknowledgments
Inspired by the work of PrayTimes.org.

Special thanks to the Muslim astronomy community for their contributions to prayer time calculations.

Support
If you encounter any issues or have questions, please open an issue on GitHub.

Show Your Support
⭐️ If you find this library useful, please give it a star on GitHub!

This README.md provides a comprehensive overview of your library, making it easy for users to understand, install, and use. It also encourages contributions and acknowledges the community’s efforts. 🚀
