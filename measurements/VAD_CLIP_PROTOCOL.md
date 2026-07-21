# VAD sınır inceleme protokolü

Amaç, RMS baseline VAD'ın gerçek cihazda sessizliği STT'ye göndermediğini ve konuşmanın ilk/son hecelerini koruduğunu doğrulamaktır.

## Klip seti

- 7 sessiz klip: farklı odalar ve cihaz yönleri, 5–10 saniye.
- 7 konuşmalı klip: kısa/uzun cümle, düşük/normal ses, 1 saniye başlangıç sessizliği.
- 6 rüzgarlı klip: fan veya dış ortam, konuşmasız ve konuşmalı örnekler.

Toplam 20 ham klip 16 kHz, mono, signed PCM 16-bit little-endian olmalıdır. İlk 1 saniyede konuşma olmamalıdır; bu bölüm cihaz ve ortam baseline kalibrasyonudur.

## Kabul ölçütleri

- Sessiz kliplerde segment sayısı `0`.
- Konuşmalı kliplerde başlangıç, elle işaretlenen ilk sesten en fazla 300 ms önce; duyulan ilk hece kesilmemeli.
- Bitiş, elle işaretlenen son sesten yaklaşık 500 ms sonra; son hece kesilmemeli.
- Rüzgarlı konuşmasız kliplerde yanlış segment oranı raporlanmalı; sürekli rüzgâr kalibrasyona dahil edilmelidir.
- Her klip için `clip, category, start_ms, end_ms, noise_floor_rms, threshold_rms` değerleri kaydedilmelidir.

`PcmClipBoundaryAnalyzer` uygulamadaki dedektör ve segmenter ile aynı kodu kullanır; sonuçlar CSV'ye aktarılarak elle dinlenen sınırlarla karşılaştırılır.
