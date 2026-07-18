# AudioCapture test raporu

- Tarih: 2026-07-18
- Cihaz: Samsung Galaxy S24 FE (SM-S721B)
- Android: 16 / API 36
- Biçim: 16.000 Hz, mono, signed PCM 16-bit little-endian
- Bayt hızı: 32.000 byte/sn

## Kısa örnekler

| Dosya | Süre | RMS | Sessiz | Clipping |
|---|---:|---:|---|---:|
| `sample_1.pcm` | 10,80 sn | 0,1876 | Hayır | %1,4120 |
| `sample_2.pcm` | 2,00 sn | 0,0654 | Hayır | %0,0000 |
| `sample_3.pcm` | 2,00 sn | 0,0507 | Hayır | %0,0000 |
| `sample_4.pcm` | 1,52 sn | 0,0694 | Hayır | %0,0000 |
| `sample_5.pcm` | 1,60 sn | 0,0632 | Hayır | %0,0000 |

Sessizlik eşiği RMS `< 0,01`, clipping eşiği `|sample| >= 32760` olarak kullanıldı. İlk örnekte clipping bulundu; test amacıyla korunabilir ancak model girdisi olarak kullanılacaksa daha uzaktan/normal sesle yeniden kaydedilmelidir.

## 30 saniye geçiş testi

| Ölçüm | Sonuç |
|---|---:|
| Dosya | `stability_30s.pcm` |
| Gerçek boyut | 956.800 byte |
| Teorik boyut | 960.000 byte |
| Yaklaşık ses süresi | 29,90 sn |
| Buffer kapsamı | %99,67 |
| Uygulama çökmesi | Yok |
| AndroidRuntime fatal kayıt | Yok |
| Test sonrası süreç | Çalışıyor |

**Geçiş sonucu: BAŞARILI.** Kayıt sırasında çökme veya read-loop kopması gözlenmedi. Başlatma/durdurma sınırındaki yaklaşık 100 ms fark kabul edilebilir; sürekli buffer kaybı işareti yok.

## Saklama konumu

Test PCM dosyaları cihazda uygulamaya özel `files/test_audio` dizinindedir. Bu seçim başka uygulamaların seslere izinsiz erişmesini engeller ve veri saklama politikasıyla uyumludur.
