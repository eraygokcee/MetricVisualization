// CameraController.cs
using UnityEngine;

public class CameraController : MonoBehaviour
{
    [Header("Hareket Ayarları")]
    public float moveSpeed = 50f;
    public float rotSpeed = 80f;
    public float zoomSpeed = 1000f;

    [Header("Sınırlama Ayarları")]
    [Tooltip("Kameranın araziden en az ne kadar yukarıda olacağı.")]
    public float minHeightAboveTerrain = 2f;
    [Tooltip("Kameranın bir engele çarpmadan ne kadar önce duracağı.")]
    public float collisionPadding = 2f;

    // CubeSpawner tarafından atanacak olan terrain referansı
    [HideInInspector]
    public Terrain targetTerrain;

    void Update()
    {
        // --- HAREKET HESAPLAMALARI ---

        // WASD ile yön vektörünü al
        Vector3 moveDirection = new Vector3(
            Input.GetAxis("Horizontal"), // A/D
            0,
            Input.GetAxis("Vertical")   // W/S
        );

        // Yukarı/aşağı (Space / LeftCtrl)
        float upDown = 0;
        if (Input.GetKey(KeyCode.Space)) upDown = 1f;
        if (Input.GetKey(KeyCode.LeftControl)) upDown = -1f;

        // Fare tekerleği ile zoom
        float scroll = Input.GetAxis("Mouse ScrollWheel");
        moveDirection.z += scroll * zoomSpeed * Time.deltaTime;


        // --- ÇARPIŞMA KONTROLÜ (İLERİ/GERİ) ---

        // Eğer ileri veya geri hareket varsa (W,S veya zoom)
        if (Mathf.Abs(moveDirection.z) > 0.01f)
        {
            // İleri yönde bir ışın gönder
            Ray ray = new Ray(transform.position, transform.forward);

            // Eğer ışın bir engele çarparsa ve bu engel çok yakındaysa
            if (Physics.Raycast(ray, out RaycastHit hit, collisionPadding))
            {
                // Sadece ileri hareketi engelle, geri harekete izin ver
                if (moveDirection.z > 0)
                {
                    moveDirection.z = 0; // İleri hareketi sıfırla
                }
            }
        }

        // --- HAREKETİN UYGULANMASI ---

        // Hesaplanan yön ve hız ile kamerayı hareket ettir
        transform.Translate(moveDirection * moveSpeed * Time.deltaTime, Space.Self);
        transform.Translate(Vector3.up * upDown * moveSpeed * Time.deltaTime, Space.World);


        // --- DÖNME İŞLEMİ ---

        // Fare sağ tuş basılıyken döndür
        if (Input.GetMouseButton(1))
        {
            float h = rotSpeed * Input.GetAxis("Mouse X") * Time.deltaTime;
            float v = rotSpeed * Input.GetAxis("Mouse Y") * Time.deltaTime;
            transform.Rotate(Vector3.up, h, Space.World);
            transform.Rotate(Vector3.right, -v, Space.Self);
        }

        // --- YÜKSEKLİK SINIRLAMASI (YERE GİRMEYİ ENGELLEME) ---

        ApplyHeightLimit();
    }

    private void ApplyHeightLimit()
    {
        // Eğer bir terrain atanmamışsa bu fonksiyonu çalıştırma
        if (targetTerrain == null) return;

        // Kameranın mevcut pozisyonunu al
        Vector3 pos = transform.position;

        // Kameranın altındaki arazinin dünya yüksekliğini al
        // Terrain.SampleHeight, verilen dünya pozisyonundaki arazinin Y değerini döner.
        float terrainHeight = targetTerrain.SampleHeight(pos);

        // Olması gereken minimum yüksekliği hesapla
        float minAllowedHeight = terrainHeight + minHeightAboveTerrain;

        // Eğer kamera olması gerekenden daha alçaktaysa, onu minimum yüksekliğe çek
        if (pos.y < minAllowedHeight)
        {
            pos.y = minAllowedHeight;
            transform.position = pos;
        }
    }
}