# Karar günlüğü

## ADR-001 — Kotlin ve Jetpack Compose

- Tarih: 2026-07-18
- Durum: Kabul edildi
- Karar: Tek modüllü Kotlin/Compose Android uygulamasıyla başla.
- Gerekçe: Küçük MVP'de hızlı UI iterasyonu ve sade proje yapısı.

## ADR-002 — Önce intent tabanlı entegrasyon

- Tarih: 2026-07-18
- Durum: Kabul edildi
- Karar: Harita, arama, mesaj ve medya eylemlerinde mümkün olduğunca Android intent/medya oturumu kullan.
- Gerekçe: Sağlayıcı bağımlılığını ve ayrıcalıklı izinleri azaltır.

## ADR-003 — Hassas eylemlerde açık onay

- Tarih: 2026-07-18
- Durum: Kabul edildi
- Karar: Arama, mesaj ve konum paylaşımı yürütülmeden önce hedef ve içerik gösterilir.
- Gerekçe: Yanlış tanımanın gerçek dünya etkisini sınırlar.

## ADR-004 — İçeriksiz ölçüm

- Tarih: 2026-07-18
- Durum: Kabul edildi
- Karar: Ham ses, ifade, kişi, numara ve kesin konum analitiğe yazılmaz.
- Gerekçe: Kalite sinyali korunurken mahremiyet riski azalır.

