# Tehdit modeli

## Kapsam ve varlıklar

Korunan varlıklar: ses/metin komutu, kişi ve telefon numarası, konum, uygulama kullanım bilgisi, hesap/sağlayıcı bağlantıları ve eylem geçmişi. Güven sınırları cihaz, Android intent sistemi, konuşma tanıma sağlayıcısı ve Spotify/YouTube gibi üçüncü taraflardır.

## Başlıca tehditler ve kontroller

| Tehdit | Etki | İlk kontrol |
|---|---|---|
| Yanlış duyulan komut | Yanlış arama/mesaj/eylem | Kritik eylem önizlemesi ve açık onay |
| Kilit ekranından kötüye kullanım | Yetkisiz işlem | Hassas eylemde cihaz kimlik doğrulaması |
| Sahte/çakışan kişi adı | Yanlış alıcı | Numara/kişi ayrıntısını göster, belirsizlikte seçim iste |
| Komut geçmişinin sızması | Mahremiyet kaybı | Varsayılan olarak içerik geçmişi tutma |
| Loglarda kişisel veri | Kalıcı sızıntı | Üretimde içerik loglama kapalı, kimliksiz olay kodları |
| Zararlı intent/paket | Uygulama yönlendirme | Sabit allowlist, çözümleme kontrolü, açık intent tercihi |
| Üçüncü taraf veri toplama | Profil çıkarma | En az veri gönderimi ve sağlayıcı açıklaması |
| Tekrar oynatma/enjeksiyon | İstenmeyen eylem | Kısa oturum, görünür mikrofon göstergesi, onay |
| Aşırı izin | Geniş saldırı alanı | İzni yalnız özellik kullanılırken iste |

## Güvenlik ilkeleri

- `CALL_PHONE` yerine mümkünse arama ekranını aç; doğrudan gönderim yerine mesaj taslağı oluştur.
- Mikrofon, kişiler ve konumu kurulumda topluca değil, ilgili komutta iste.
- Fener gibi geri alınabilir düşük riskli eylemler doğrudan; mesaj, arama ve konum paylaşımı onaylıdır.
- Gizli anahtarlar repoya veya APK içine konmaz; sunucu tarafı sırları ayrı tutulur.

