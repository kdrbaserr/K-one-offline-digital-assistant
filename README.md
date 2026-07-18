# Kone Assistant

Türkçe Android komut yardımcısı için güvenlik ve ölçüm odaklı MVP iskeleti. İlk sürüm; harita, arama, mesaj, Spotify, YouTube, uygulama açma, fener ve zamanlayıcı alanlarında 16 komut tanımlar.

## Gün sonu çıktısı

- Kotlin/Jetpack Compose tek modüllü uygulama iskeleti
- [`docs/COMMAND_CATALOG.md`](docs/COMMAND_CATALOG.md): 16 komut ve kabul ölçütleri
- [`docs/DEVICE_PROFILE.md`](docs/DEVICE_PROFILE.md): hedef telefon/API/donanım kaydı
- [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md): tehditler ve kontroller
- [`docs/DATA_RETENTION.md`](docs/DATA_RETENTION.md): veri saklama politikası
- [`docs/DECISIONS.md`](docs/DECISIONS.md): karar günlüğü
- [`measurements/command_trials.csv`](measurements/command_trials.csv): deneme kayıt şablonu
- [`measurements/AUDIO_CAPTURE_REPORT.md`](measurements/AUDIO_CAPTURE_REPORT.md): PCM örnek ve 30 saniye kararlılık sonuçları

## Başlatma

Gerekenler: Android Studio, Android SDK 37 ve Android SDK Platform Tools. Proje Android Studio ile açılıp Gradle senkronizasyonu yapılır. İlk derleme, kablosuz ADB bağlantısı ve `0.1.0` sürümünün Galaxy S24 FE üzerine kurulumu doğrulandı.

> Not: `gradle-wrapper.properties` hazırdır; Gradle Wrapper JAR ve çalıştırma betikleri Android Studio'da **Gradle > Wrapper** göreviyle veya yerel Gradle 9.5.0 ile `gradle wrapper` çalıştırılarak üretilmelidir.

Hedef telefonu kaydetmek için USB hata ayıklamayı açın, telefonu bağlayın ve:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/capture-device.ps1
```

## İlk uygulama sırası

1. Hedef cihaz profilini doldur ve temel gecikmeyi ölç.
2. Metin girdisiyle niyet/parametre ayrıştırıcıyı geliştir; ses katmanını sonra bağla.
3. Düşük riskli `uygulama aç` ve `fener` eylemlerini uygula.
4. Arama/mesaj/konum için önizleme ve onay akışını uygula.
5. Katalogdaki her komutu ölçüm CSV'siyle en az 10 kez dene.

## Mahremiyet özeti

Ham ses ve komut metni varsayılan olarak saklanmaz. Analitik açıksa yalnız içeriksiz komut ID'si, sonuç sınıfı ve kaba gecikme en fazla 30 gün tutulur. Hassas eylemler kullanıcı onayı ister.
