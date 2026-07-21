# Türkçe STT motor karşılaştırması

Tarih: 2026-07-21  
Cihaz: Samsung SM-S721B, Android 16, arm64-v8a  
Korpus: Aynı 60 gerçek saha klibi, 16 kHz mono PCM16, 0,7–2,9 saniye

## Sonuç

| Motor | Klip | Gecikme p50 | Gecikme p95 | Bellek p50 / tepe | Model boyutu | Isı başlangıç → bitiş |
|---|---:|---:|---:|---:|---:|---:|
| Vosk small-tr 0.3 | 60 | 346 ms | 724 ms | 201,9 / 221,7 MiB PSS | 35,1 MiB zip; 56,2 MiB kurulu | 41,2 → 41,2 °C |
| whisper.cpp tiny q5_1 | 60 | 1.992 ms | 2.216 ms | 130,0 / 130,6 MiB Max RSS | 30,7 MiB | 40,9 → 42,8 °C |
| whisper.cpp base q5_1 | 60 | 4.149 ms | 7.770 ms | 196,8 / 198,2 MiB Max RSS | 56,9 MiB | 42,8 → 44,4 °C |

Bu cihaz ve kısa komut senaryosunda ana runtime olarak **Vosk small-tr 0.3** seçildi. Vosk p50 gecikmede tiny’den yaklaşık 5,8 kat, base’den yaklaşık 12 kat hızlıdır. Uygulamadaki mevcut Vosk yolu ana ve tek üretim STT yolu olarak bırakıldı; whisper.cpp uygulamaya ikinci üretim runtime’ı olarak eklenmedi.

## Başarı oranı hakkında zorunlu veri notu

Bu 60 klibin beklenen metin/intent etiketi yoktur. Kliplerin çoğu sessizlik veya müzik içerir: Vosk 60 klibin 10’unda boş olmayan metin, Whisper ise çoğunlukla `[MÜZİK ÇALIYOR]` benzeri açıklama üretti. Bunları doğru komut kabul etmek bilimsel olarak yanlış olacağından **komut başarı oranı bu korpusta N/A** olarak kaydedildi.

Kararın doğruluk ayağını kapatmak için en az 50 kliplik `clip, expected_intent, expected_slots` manifesti gerekir. Aynı ham sonuçlar bu manifestle eşleştirildiğinde başarı oranı yeniden hesaplanmalı; Vosk hedef komutlarda kabul eşiğinin altına düşerse ADR yeniden açılmalıdır.

## Yöntem ve sınırlamalar

- Her motor aynı ada sahip 60 ses parçasını aynı cihazda çalıştırdı.
- Vosk modeli bir kez yüklenip 60 klip boyunca bellekte tutuldu; süre yalnız tanıma oturumunu kapsar.
- whisper.cpp CLI her klipte ayrı prosesle soğuk başlatıldı; model yükleme süresi gecikmeye dahildir. Üretimde modeli bellekte tutan JNI oturumu daha hızlı olacaktır.
- Vosk belleği Android uygulamasının toplam PSS’idir; Whisper belleği native prosesin Max RSS değeridir. Rakamlar yön gösterir, birebir aynı bellek metriği değildir.
- Testler arka arkaya çalıştığı için sıcaklık başlangıçları eşit değildir. Base testi tiny’den sonra başladığından termal sonuç yalnız bu test sırasını temsil eder.
- Whisper’ın resmi Android örneği `arm64-v8a` için derlendi; oluşan örnek release APK 31,4 MiB’tır. tiny ve base modelleri cihaz üzerinde başarıyla açılıp aynı kliplerde çalıştı.

## Tekrarlama

- Vosk cihaz testi: `VoskClipBenchmarkTest`
- Whisper cihaz testi: `scripts/run-whisper-device-benchmark.ps1`
- Ham veri: `stt-vosk-raw.csv` ve `stt-whisper-raw.csv`
- Model sürüm/checksum kaydı: `whisper-models.json`

