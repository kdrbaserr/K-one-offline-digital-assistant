# Veri saklama politikası

## Varsayılan yaklaşım

Komut içeriği kalıcı olarak saklanmaz. İşleme mümkün olduğunca cihazda yapılır. Geçici ses tamponu sonuç üretildikten veya işlem iptal edildikten hemen sonra silinir.

| Veri | Amaç | Saklama | Silme |
|---|---|---|---|
| Ham ses | Konuşmayı çözme | Yalnız işlem süresi | Oturum sonunda otomatik |
| Komut metni | Niyet/parametre çıkarma | Yalnız işlem süresi | Oturum sonunda otomatik |
| Kişi/numara/konum | Eylem hedefi | Yalnız işlem süresi | Oturum sonunda otomatik |
| Ölçüm olayı | Kalite ve hata oranı | 30 gün, içeriksiz | Periyodik otomatik |
| Çökme kaydı | Kararlılık | 30 gün, kişisel veri ayıklanmış | Periyodik otomatik |
| Kullanıcı tercihi | Ürün davranışı | Kullanıcı silene kadar | Ayarlar / uygulama verisini sil |

Kullanıcı ölçüm paylaşımını kapatabilir. Ölçüm olayları yalnız komut ID'si, başarı/hata sınıfı, kaba gecikme ve uygulama sürümünü içerir; ifade, kişi, numara ve kesin konum içermez. Politika veya süre değişikliği karar günlüğüne yazılır ve uygulama içinde açıklanır.

