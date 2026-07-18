# Ölçüm şablonu

Her komut için sessiz, gürültülü ve ekran kilitli/uygulama arka planda gibi uygun koşullarda en az 10 deneme kaydedin. Kişisel veri yerine anonim test kişileri ve sahte numaralar kullanın.

Temel metrikler:

- Niyet doğruluğu = `intent_correct / toplam deneme`
- Parametre doğruluğu = `slots_correct / toplam deneme`
- Uçtan uca başarı = `action_success / toplam deneme`
- Güvenlik uyumu = hassas eylemlerde `confirmation_shown` oranı
- Gecikme = medyan ve p95 `latency_ms`

Hata sınıfları: `NO_SPEECH`, `ASR`, `WRONG_INTENT`, `WRONG_SLOT`, `PERMISSION`, `NO_HANDLER`, `USER_CANCEL`, `ACTION_FAILED`.

