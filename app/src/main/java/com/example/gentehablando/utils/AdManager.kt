package com.example.gentehablando.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interstitialAd: InterstitialAd? = null
    private var lastAdShowTime: Long = 0L
    private var isAdLoading = false

    // 5 minutos de tiempo de gracia (cooldown)
    private val AD_COOLDOWN_MS: Long = 5 * 60 * 1000L 
    
    // Unidad de Anuncio de PRUEBA para Interstitial en Android
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // i18n: technical Google public test ad-unit ID; SDK configuration.

    init {
        loadAd()
    }

    private fun loadAd() {
        if (interstitialAd != null || isAdLoading) {
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TEST_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("AdManager", "Error cargando anuncio: ${adError.message}") // i18n: technical Diagnostic log tag/message; never displayed.
                    interstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdManager", "Anuncio cargado en memoria exitosamente.") // i18n: technical Diagnostic log tag/message; never displayed.
                    interstitialAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    /**
     * Intenta mostrar el anuncio si ha pasado el tiempo de enfriamiento (5 minutos).
     * @param activity Actividad en pantalla requerida por AdMob
     * @param onAdClosed Opcional: Función a ejecutar justo cuando el usuario cierre/salte el anuncio.
     */
    fun showAdIfCoolDownPassed(activity: Activity, onAdClosed: () -> Unit = {}) {
        val now = System.currentTimeMillis()

        // 1. Verificar si ya pasaron los 5 minutos (y si tenemos anuncio en memoria)
        if (now - lastAdShowTime >= AD_COOLDOWN_MS && interstitialAd != null) {
            
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // El usuario le dio a la (X) o cerró el anuncio
                    interstitialAd = null
                    lastAdShowTime = System.currentTimeMillis() // Reiniciamos el reloj de 5 mins
                    loadAd() // Empezamos a cargar el siguiente por debajo
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    interstitialAd = null
                    loadAd()
                    onAdClosed() // Si falla, lo dejamos pasar limpiamente
                }
                
                override fun onAdShowedFullScreenContent() {
                    // Anuncio mostrándose actualmente en pantalla
                }
            }

            interstitialAd?.show(activity)
            
        } else {
            // Si no han pasado 5 minutos o no cargó el anuncio, lo dejamos continuar sin interrumpir
            onAdClosed()
        }
    }
}
