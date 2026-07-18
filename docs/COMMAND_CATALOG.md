# Komut kataloğu

İlk sürüm 8 alanda 16 komutu kapsar. Dış dünyada etkisi olan arama, mesaj, konum paylaşımı ve uygulama açma eylemleri kullanıcı onayı olmadan çalıştırılmaz.

| ID | Alan | Örnek ifade | Beklenen sonuç | Başarı ölçütü |
|---|---|---|---|---|
| MAP-01 | Harita | Eve yol tarifi başlat | Kayıtlı ev adresine navigasyon | Doğru hedef ve intent |
| MAP-02 | Harita | En yakın eczaneyi bul | Yakındaki eczaneleri göster | Doğru sorgu ve konum |
| CALL-01 | Arama | Annemi ara | Kişi seçilip arama ekranı açılır | Doğru kişi, onay var |
| CALL-02 | Arama | 0212 555 00 00'ı ara | Numara arama ekranında açılır | Numara kayıpsız |
| MSG-01 | Mesaj | Ayşe'ye gecikeceğim yaz | Alıcı/metin önizlemesi | Gönderim öncesi onay |
| MSG-02 | Mesaj | Mehmet'e konumumu gönder | Konum paylaşım önizlemesi | Alıcı ve konum onayı |
| SPOT-01 | Spotify | Spotify'da caz çal | Caz araması/oynatması | Doğru sağlayıcı/sorgu |
| SPOT-02 | Spotify | Müziği duraklat | Medya duraklar | Oynatma durumu değişir |
| YT-01 | YouTube | YouTube'da Kotlin dersi ara | Video araması | Sorgu kayıpsız |
| YT-02 | YouTube | Sonraki videoya geç | Sonraki medya | Aktif oturum değişir |
| APP-01 | Uygulama | WhatsApp'ı aç | Uygulama açılır | Doğru paket |
| APP-02 | Uygulama | Kamerayı aç | Kamera açılır | Kamera intent'i çözülür |
| TORCH-01 | Fener | Feneri aç | Fener açılır | Donanım durumu açık |
| TORCH-02 | Fener | Feneri kapat | Fener kapanır | Donanım durumu kapalı |
| TIMER-01 | Zamanlayıcı | 10 dakikalık zamanlayıcı kur | Zamanlayıcı kurulur | Süre ±1 sn |
| TIMER-02 | Zamanlayıcı | Zamanlayıcıyı iptal et | Aktif sayaç iptal edilir | Sayaç kalmaz |

Her komut için en az 10 Türkçe ifade varyasyonu ve gürültülü/sessiz ortam örnekleri sonraki veri toplama turunda eklenir.

