# MinIO File Service

Bu proje, Spring Boot, Gradle ve Java 17 kullanılarak geliştirilmiş, MinIO ile entegre bir dosya yönetim mikroservisidir.

## Özellikler

- **MinIO Entegrasyonu**: Dosya yükleme, görüntüleme, silme ve düzenleme işlemleri
- **Resim Boyutlandırma**: Yüklenen resimlerin dinamik olarak belirtilen boyutlarda (16x16, 32x32, 64x64 vb.) otomatik olarak yeniden boyutlandırılması
- **Yetkilendirme**: OpenFGA ile dosya ve dizinlere kullanıcı bazlı erişim kontrolü
- **Dosya Paylaşımı**: Geçici paylaşım linkleri oluşturma ve yönetme
- **Kullanıcı Yönetimi**: Keycloak entegrasyonu ile kullanıcı kimlik doğrulama ve yetkilendirme
- **Veritabanı**: PostgreSQL ile veri saklama
- **API Dokümantasyonu**: OpenAPI (Swagger) ile API dokümantasyonu
- **Test ve İstisna Yönetimi**: Kapsamlı birim testleri ve istisna yönetimi
- **Virüs Tarama**: VirusTotal API entegrasyonu ile dosya güvenliği kontrolü

## Teknolojiler

- Java 17
- Spring Boot 3.1.5
- Gradle
- MinIO 8.5.4
- OpenFGA 0.2.0
- Keycloak 21.1.1
- PostgreSQL
- Flyway
- Thumbnailator (Resim işleme)
- VirusTotal API (Virüs tarama)
- JUnit 5 & Mockito (Test)
- Swagger/OpenAPI (API Dokümantasyonu)

## Proje Yapısı

```
minio-file-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── fileservice/
│   │   │           └── minioservice/
│   │   │               ├── config/         # Konfigürasyon sınıfları
│   │   │               ├── controller/     # REST API kontrolcüleri
│   │   │               ├── dto/            # Veri transfer objeleri
│   │   │               ├── exception/      # İstisna sınıfları
│   │   │               ├── model/          # Veri modelleri
│   │   │               ├── repository/     # Veritabanı repository'leri
│   │   │               ├── service/        # İş mantığı servisleri
│   │   │               ├── security/       # Güvenlik konfigürasyonu
│   │   │               ├── util/           # Yardımcı sınıflar
│   │   │               └── MinioServiceApplication.java  # Ana uygulama sınıfı
│   │   └── resources/
│   │       ├── application.yml             # Ana konfigürasyon dosyası
│   │       ├── application-postgres.yml    # PostgreSQL konfigürasyonu
│   │       ├── application-virustotal.yml  # VirusTotal konfigürasyonu
│   │       └── db/
│   │           └── migration/              # Flyway veritabanı migrasyon dosyaları
│   └── test/
│       └── java/
│           └── com/
│               └── fileservice/
│                   └── minioservice/
│                       └── service/        # Servis birim testleri
├── build.gradle                            # Gradle yapılandırması
└── settings.gradle                         # Gradle proje ayarları
```

## Kurulum ve Çalıştırma

### Ön Koşullar

- Java 17 veya üzeri
- Gradle 7.0 veya üzeri
- PostgreSQL 12 veya üzeri
- MinIO Sunucusu
- Keycloak Sunucusu
- OpenFGA Sunucusu
- VirusTotal API Anahtarı

### Veritabanı Kurulumu

PostgreSQL'de `fileservice` adında bir veritabanı oluşturun:

```sql
CREATE DATABASE fileservice;
```

### Konfigürasyon

`application.yml` dosyasında aşağıdaki ayarları kendi ortamınıza göre düzenleyin:

- MinIO bağlantı bilgileri
- PostgreSQL bağlantı bilgileri
- Keycloak bağlantı bilgileri
- OpenFGA bağlantı bilgileri
- VirusTotal API anahtarı

### VirusTotal API Konfigürasyonu

`application-virustotal.yml` dosyasında VirusTotal API ayarlarını yapılandırın:

```yaml
virustotal:
  api-key: your-virustotal-api-key
  api-url: https://www.virustotal.com/api/v3
  enabled: true
  scan-timeout: 60000  # 60 seconds timeout for scan operations
```

### Derleme

```bash
./gradlew clean build
```

### Çalıştırma

```bash
./gradlew bootRun
```

veya

```bash
java -jar build/libs/minio-file-service-0.0.1-SNAPSHOT.jar
```

## API Dokümantasyonu

Uygulama çalıştırıldıktan sonra Swagger UI'a aşağıdaki URL üzerinden erişilebilir:

```
http://localhost:8081/api/swagger-ui.html
```

API dokümantasyonu aşağıdaki URL üzerinden erişilebilir:

```
http://localhost:8081/api/api-docs
```

## API Endpoints

### Dosya İşlemleri

- `POST /api/files`: Dosya yükleme
- `GET /api/files`: Kullanıcının tüm dosyalarını listeleme
- `GET /api/files/{id}`: Dosya meta verilerini getirme
- `GET /api/files/{id}/content`: Dosya içeriğini indirme
- `DELETE /api/files/{id}`: Dosya silme
- `PATCH /api/files/{id}`: Dosya meta verilerini güncelleme
- `GET /api/files/search`: Dosya adına göre arama

### Resim İşlemleri

- `POST /api/images/validate`: Dosyanın resim olup olmadığını doğrulama
- `GET /api/images/dimensions/presets`: Önceden tanımlanmış resim boyutlarını listeleme

### İzin Yönetimi

- `GET /api/permissions/files/{fileId}`: Dosya izinlerini listeleme
- `POST /api/permissions`: Kullanıcıya izin verme
- `DELETE /api/permissions`: Kullanıcıdan izin kaldırma
- `GET /api/permissions/my-access`: Kullanıcının erişim izni olan tüm dosyaları listeleme

### Paylaşım İşlemleri

- `POST /api/shares`: Paylaşım linki oluşturma
- `GET /api/shares`: Kullanıcının oluşturduğu tüm paylaşım linklerini listeleme
- `GET /api/shares/files/{fileId}`: Dosya için oluşturulan tüm paylaşım linklerini listeleme
- `DELETE /api/shares/{token}`: Paylaşım linkini silme
- `GET /api/shares/access/{token}`: Paylaşım linki ile dosya meta verilerine erişme
- `GET /api/shares/access/{token}/content`: Paylaşım linki ile dosya içeriğini indirme
- `GET /api/shares/validate/{token}`: Paylaşım linkinin geçerliliğini kontrol etme

### Virüs Tarama İşlemleri

- `GET /api/virus-scan/files/{fileId}`: Dosya için virüs tarama sonucunu getirme

## Virüs Tarama Özelliği

Bu mikroservis, VirusTotal API entegrasyonu ile dosya güvenliği kontrolü sağlar. Dosya yükleme işlemi sırasında, dosya önce VirusTotal'a gönderilir ve tarama sonucu alınır. Eğer dosya güvenli ise (virüs içermiyorsa) yükleme işlemi devam eder. Eğer dosya virüs içeriyorsa, yükleme işlemi reddedilir ve kullanıcıya uygun bir hata mesajı gösterilir.

Virüs tarama sonuçları veritabanında saklanır ve daha sonra sorgulanabilir. Bu sayede, daha önce taranmış dosyaların güvenlik durumu hakkında bilgi alınabilir.

### Virüs Tarama Sonuçları

Virüs tarama sonuçları aşağıdaki bilgileri içerir:

- `scanned`: Dosyanın taranıp taranmadığı
- `clean`: Dosyanın temiz olup olmadığı (virüs içermediği)
- `positives`: Kaç antivirüs motorunun virüs tespit ettiği
- `total`: Toplam kaç antivirüs motorunun dosyayı taradığı
- `scanId`: VirusTotal tarama kimliği
- `resource`: Taranan dosyanın kaynak kimliği
- `permalink`: VirusTotal'da tarama sonucuna doğrudan erişim linki
- `message`: Tarama sonucu ile ilgili mesaj

## Güvenlik

Bu uygulama, OAuth 2.0 / OpenID Connect protokollerini kullanarak Keycloak üzerinden kimlik doğrulama ve yetkilendirme sağlar. API'ye erişmek için geçerli bir JWT token gereklidir.

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.
