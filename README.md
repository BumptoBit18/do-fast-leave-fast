# AuctionHub

## 1. Mo ta bai toan va pham vi he thong

AuctionHub la he thong dau gia truc tuyen xay dung theo kien truc Client-Server. He thong ho tro nguoi ban tao va quan ly phien dau gia, nguoi mua tham gia dau gia theo thoi gian thuc, va quan tri vien giam sat nguoi dung, giao dich, thong bao va cac yeu cau nap tien.

Pham vi he thong trong do an nay gom:

- Client desktop JavaFX cho `Bidder`, `Seller`, `Admin`
- Server Java xu ly nghiep vu va giao tiep voi database
- Giao tiep Client-Server qua TCP socket
- Luu tru du lieu bang PostgreSQL
- Kiem thu va CI bang Maven + GitHub Actions

## 2. Cong nghe su dung, moi truong chay va yeu cau cai dat

### Cong nghe

- Java 21
- JavaFX 21.0.6
- Maven
- PostgreSQL
- JUnit 5
- JaCoCo
- Checkstyle
- GitHub Actions
- Qodana

### Moi truong chay

- Windows: su dung `run-server.ps1`, `run-app.ps1`
- Linux: su dung `run-server-linux.sh`, `run-app-linux.sh`
- macOS: su dung `run-server-macos.sh`, `run-app-macos.sh`

### Yeu cau cai dat

- JDK 21
- Maven 3.9+
- PostgreSQL
- Bien moi truong `JAVA_HOME` da duoc cau hinh
- Maven dependencies da duoc tai ve trong thu muc `.m2`

### Cau hinh database

Tao file `config/database.properties` dua tren file mau `config/database.properties.example`, hoac cau hinh bang:

- System properties `auction.db.*`
- Bien moi truong `AUCTION_DB_*`

Server la thanh phan duy nhat ket noi truc tiep voi database.

## 3. Cau truc thu muc va cac module chinh

```text
do-fast-leave-fast/
|-- client/                 # Ma nguon va tai nguyen giao dien client JavaFX
|   |-- java/
|   `-- resources/
|-- src/main/java/          # Ma nguon dung chung va khoi dong client
|-- server/                 # Thanh phan server, controller, DAO, model, network
|-- config/                 # Cau hinh database, checkstyle, file cau hinh khac
|-- data/                   # Du lieu legacy phuc vu migrate khi can
|-- .github/workflows/      # CI va code quality workflow
|-- run-server.ps1          # Script chay server tren Windows
|-- run-app.ps1             # Script chay client tren Windows
|-- run-server-linux.sh     # Script chay server tren Linux
|-- run-app-linux.sh        # Script chay client tren Linux
|-- run-server-macos.sh     # Script chay server tren macOS
|-- run-app-macos.sh        # Script chay client tren macOS
`-- pom.xml                 # Cau hinh Maven, test, coverage, checkstyle
```

## 4. Cau lenh dong lenh de chay chuong trinh

Luu y:

- Cac script duoi day da ton tai san trong repo cho Windows, Linux va macOS.
- Trong moi he dieu hanh, can chay lenh tai thu muc goc cua du an `do-fast-leave-fast`.
- Linux/macOS can cap quyen thuc thi cho file shell script truoc lan dau su dung.

### Tai dependencies truoc khi chay

```bash
mvn dependency:go-offline
```

### Windows

Chay Server:

```powershell
.\run-server.ps1
```

Chay Client:

```powershell
.\run-app.ps1
```

Client ket noi toi server khac:

```powershell
.\run-app.ps1 -ServerHost <server-host> -Port 5050
```

### Linux

Cap quyen chay script:

```bash
chmod +x run-server-linux.sh run-app-linux.sh
```

Chay Server:

```bash
./run-server-linux.sh
```

Chay Client:

```bash
./run-app-linux.sh --server-host localhost --port 5050
```

### macOS

Cap quyen chay script:

```bash
chmod +x run-server-macos.sh run-app-macos.sh
```

Chay Server:

```bash
./run-server-macos.sh
```

Chay Client:

```bash
./run-app-macos.sh --server-host localhost --port 5050
```

## 5. Huong dan chay Server/Client theo thu tu cu the

### Buoc 1. Clone repo va di chuyen vao thu muc du an

```bash
git clone <repo-url>
cd do-fast-leave-fast
```

### Buoc 2. Cau hinh database

- Tai PostgreSQL phu hop voi he dieu hanh may tinh: 
  - Mo phan mem `pgAdmin` (cai san kem theo PostgreSQL) .
  - O cot ben trai `Browser`, mo rong cay thu muc `Servers -> PostgreSQL` .
  - Nhap mat khau (neu duoc yeu cau) de ket noi voi server.
  - Nhap chuot phai vao much Database.
  - Chon `Create -> Database...`
  - Mot cua so hien ra -> O tab `General` nhap ten database muon tao vao o Database
  - Nhan save Database se xuat hien o thanh ben trai 
- Sua cau hinh db trong file `config/database.properties`

### Buoc 3. Tai dependencies

```bash
mvn dependency:go-offline
```

### Buoc 4. Khoi dong Server

Chon lenh phu hop voi he dieu hanh:

- Windows: `.\run-server.ps1`
- Linux: `./run-server-linux.sh`
- macOS: `./run-server-macos.sh`

Khi server da mo cong `5050`, giu nguyen cua so terminal nay.

### Buoc 5. Khoi dong Client

Mo terminal thu hai va chay lenh phu hop voi he dieu hanh:

- Windows: `.\run-app.ps1`
- Linux: `./run-app-linux.sh --server-host localhost --port 5050`
- macOS: `./run-app-macos.sh --server-host localhost --port 5050`

Neu client chay tren may khac, thay `localhost` bang IP hoac domain cua may dang chay server.

### Tai khoan mau

- `bidder / bidder123`
- `seller / seller123`
- `admin / admin`

## 6. Danh sach chuc nang da hoan thanh

- Dang nhap theo 3 vai tro: `Bidder`, `Seller`, `Admin`
- Dang ky tai khoan cho `Bidder` va `Seller`
- Seller tao, xem, sua, xoa phien dau gia
- Seller tai anh san pham dinh dang `PNG`, `JPG`, `GIF`, `BMP` toi da 2 MB
- Dat gia realtime qua TCP socket
- Ho tro auto-bidding
- Ho tro anti-sniping
- Tu dong dong phien dau gia, xac dinh nguoi thang va xu ly thanh toan
- Nap tien thong qua yeu cau cho Admin phe duyet
- Admin quan ly nguoi dung, giao dich, thanh toan va thong bao
- Hien thi bieu do lich su gia realtime
- Kiem thu bang `JUnit 5`
- Do coverage bang `JaCoCo`
- Kiem tra coding convention bang `Checkstyle`
- CI bang GitHub Actions
- Static analysis bang Qodana

## 7. Test va CI/CD

Chay test va quality check bang Maven:

```bash
mvn verify
```

CI hien co trong repo:

- `.github/workflows/java-ci.yml`: build va chay `mvn verify`
- `.github/workflows/qodana_code_quality.yml`: phan tich chat luong ma nguon bang Qodana

Hien tai repo da co CI, chua co workflow CD/deploy rieng.

## 8. Link bao cao PDF va video demo

- Bao cao PDF va video demo: [Google Drive](https://drive.google.com/drive/folders/12W6M35Lm4njqpmqIvMl3RBiqyBoz0HHV?usp=sharing)

