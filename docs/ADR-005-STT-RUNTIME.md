# ADR-005 — Ana STT runtime seçimi

- Tarih: 2026-07-21
- Durum: Kabul edildi; etiketli doğruluk korpusu gelince yeniden değerlendirilecek

## Bağlam

Android uygulamasının kısa Türkçe komutları cihaz üzerinde, çevrimdışı ve düşük gecikmeyle çözmesi gerekiyor. Adaylar mevcut Vosk small-tr 0.3 ile whisper.cpp v1.9.1 tiny/base q5_1 modelleridir.

Projede `ADR-002` numarası daha önce “Önce intent tabanlı entegrasyon” kararı için kullanılmıştır. Tarihçeyi ezmemek için STT kararı sonraki boş numara olan ADR-005 ile kaydedilmiştir.

## Karar

Ana ve tek üretim STT runtime’ı olarak **Vosk small-tr 0.3** kullanılacaktır. whisper.cpp benchmark/araştırma adayı olarak kalacak, mevcut üretim akışına ikinci runtime olarak bağlanmayacaktır.

## Gerekçe

- 60 aynı kısa klipte Vosk p50/p95 346/724 ms, tiny 1.992/2.216 ms, base 4.149/7.770 ms ölçüldü.
- Kısa push-to-talk komutlarında gecikme temel ürün ölçütüdür; Vosk açık ara daha hızlıdır.
- Vosk entegrasyonu, model doğrulaması ve intent akışı halihazırda uygulamada çalışmaktadır.
- Base model daha büyük, daha yavaş ve bu seri testte daha yüksek sıcaklığa ulaşmıştır.
- Mevcut korpusta beklenen intent etiketi bulunmadığından doğruluk üstünlüğü kanıtlanmamıştır; kanıtlanmamış bir doğruluk varsayımı için üretim runtime’ı değiştirilmemiştir.

## Sonuçlar

- Uygulama paketinde ve çalışma akışında yalnız Vosk ana runtime olarak kalır.
- Whisper native ikilileri ve modelleri uygulama asset’lerine eklenmez.
- En az 50 etiketli Türkçe komut klibi hazırlandığında komut başarı oranı ölçülür. Vosk kabul eşiğini karşılamaz veya Whisper anlamlı üstünlük gösterirse bu ADR yeniden açılır.

Detaylı tablo ve yöntem: `measurements/STT_ENGINE_COMPARISON.md`.

