package com.example.neworkingsecuritypersistence.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.neworkingsecuritypersistence.R
import com.example.neworkingsecuritypersistence.models.CurrentWeather
import com.example.neworkingsecuritypersistence.networking.WeatherRepository
import com.example.neworkingsecuritypersistence.networking.WeatherRetrofitApiFactory
import com.example.neworkingsecuritypersistence.persistence.EncryptedSharedPreferencesManager
import com.example.neworkingsecuritypersistence.persistence.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Setting up our coroutine for asynchronous operations
    private val coroutineContext = Job() + Dispatchers.Default
    private val coroutineScope = CoroutineScope(coroutineContext)

    // Our weather api service we've built to fetch data with
    private val weatherRepository = WeatherRepository(WeatherRetrofitApiFactory.weatherApi)

    // Instance variables for our UI elements
    // TODO: You'll need more instance variables for your UI elements
    private lateinit var currentWeatherTextView: TextView
    private lateinit var editWeekDay: EditText
    private lateinit var currentWeatherInfo: TextView
    private lateinit var currentForecastInfo: TextView
    private lateinit var submitButton: Button
    private lateinit var currentInfo: TextView
    private lateinit var forecastInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentWeatherTextView = findViewById(R.id.current_weather)
        editWeekDay = findViewById(R.id.editWeekDay)
        currentWeatherInfo = findViewById(R.id.current_weather_info)
        currentForecastInfo = findViewById(R.id.current_forecast_info)
        submitButton = findViewById(R.id.submitButton)
        currentInfo = findViewById(R.id.current_info)
        forecastInfo = findViewById(R.id.forecast_info)

        loadSavedWeatherFromDatabase()

        // TODO: Add functionality so that the user can type in a location to search for instead of hardcoding "Halifax"
        // hint, you do NOT need to make any updates to the WeatherApi classes to do this
        //fetchCurrentWeatherForLocation("Halifax")
        configureSubmitButton()

        // TODO: Add a method to fetch the forecast for a location. Start with the TODOS in WeatherApi.kt and WeatherRepository.kt
        // see the weather api documentation: https://www.weatherapi.com/api-explorer.aspx

        // TODO: Add UI elements and display all data (including new data you need to add) from Current.kt and Location.kt
        // there are 10 in total
    }
    private fun configureSubmitButton(){
        submitButton.setOnClickListener {
            val searchSubmit = editWeekDay.text.toString()
            fetchCurrentWeatherForLocation(searchSubmit)
            fetchWeatherForecastForLocation(searchSubmit, days = 3)
        }
    }

    private fun displayCurrentWeather(currentWeather: CurrentWeather){
        val curLocationString = "Location: " + currentWeather?.location?.name + ", " + currentWeather?.location?.region + ", " + currentWeather?.location.country
        val curCordString = "Latitude: " + currentWeather?.location?.lat + ", Longitude: " + currentWeather?.location?.lon
        val curTemperatureString = "Temperature: " + currentWeather?.current?.temp_c + ".C, " + currentWeather?.current?.temp_f + ".F"
        val curWindString = "Wind: " + currentWeather?.current?.wind_mph + "MPH, " + currentWeather?.current?.wind_kph + "KPH, " + currentWeather?.current?.wind_dir
        val currentWeatherString = curLocationString + "\n" + curCordString + "\n" + curTemperatureString + "\n" + curWindString
        currentWeatherInfo.text = currentWeatherString
    }

    private fun displayWeatherForecast(weatherForecast: Any?) {
        currentForecastInfo.text = weatherForecast.toString()
    }

    // We can check if some weather data was previously saved to local storage
    private fun loadSavedWeatherFromDatabase() {
        val savedCurrentWeather = SharedPreferencesManager(this).getCurrentWeatherData()

        savedCurrentWeather?.let {
            currentWeatherInfo.text = it.toString()
        }
    }

    // Fetches the current weather from the weather api for a specific location
    private fun fetchCurrentWeatherForLocation(location: String) {

        // use our coroutine scope to execute our asynchronous task on another thread
        coroutineScope.launch {
            // hit the weather api and wait for results
            val currentWeather = weatherRepository.getCurrentWeather(location)
            Log.d("Tag", currentWeather.toString())

            // if we have a valid result, save it to local storage
            currentWeather?.let {
                saveCurrentWeatherPlaintext(it)
                saveCurrentWeatherEncrypted(it)

                // we can't update the UI from a background thread or we will crash, so we can
                // jump back to the main thread with runOnUiThread { }
                runOnUiThread {
                    //currentWeatherInfo.text = it.toString()
                    displayCurrentWeather(currentWeather)
                }
            }
        }
    }

    private fun fetchWeatherForecastForLocation(location: String, days: Int){

        coroutineScope.launch {
            val weatherForecast = weatherRepository.getForecast(location, days)
            Log.d("Tag", weatherForecast.toString())

            weatherForecast?.let {
                runOnUiThread {
                    //currentForecastInfo.text = it.toString()
                    displayWeatherForecast(weatherForecast)
                }
            }
        }

    }

    // save the current weather in plaintext to local storage
    private fun saveCurrentWeatherPlaintext(currentWeather: CurrentWeather) {
        val sharedPrefsManager = SharedPreferencesManager(this)
        sharedPrefsManager.saveCurrentWeatherData(currentWeather)
    }

    // save the current weather in an encrypted format to local storage
    private fun saveCurrentWeatherEncrypted(currentWeather: CurrentWeather) {
        val encryptedSharedPreferencesManager = EncryptedSharedPreferencesManager(this)
        encryptedSharedPreferencesManager.saveCurrentWeatherData(currentWeather)
    }
}