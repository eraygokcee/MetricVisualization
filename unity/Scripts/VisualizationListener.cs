using System;
using System.IO;
using System.Net;
using System.Text;
using UnityEngine;

public class VisualizationListener : MonoBehaviour
{
    private HttpListener _listener;
    private const string Prefix = "http://*:5000/visualize/";
    private CubeSpawner _cubeSpawner; // CubeSpawner referansı eklendi

    void Start()
    {
        _cubeSpawner = FindObjectOfType<CubeSpawner>(); // CubeSpawner'ı bul
        if (_cubeSpawner == null)
        {
            Debug.LogError("CubeSpawner sahnede bulunamadı! Lütfen Managers GameObject'ine CubeSpawner scriptini ekleyin.");
            return;
        }

        _listener = new HttpListener();
        _listener.Prefixes.Add(Prefix);
        _listener.Start();
        Debug.Log("👉 Unity HTTP Listener started on " + Prefix);
        _listener.BeginGetContext(OnRequest, null);
    }

    private void OnRequest(IAsyncResult ar)
    {
        var context = _listener.EndGetContext(ar);
        var req = context.Request;
        string body;
        using (var reader = new StreamReader(req.InputStream, req.ContentEncoding))
        {
            body = reader.ReadToEnd();
        }

        Debug.Log($"🎯 Received visualize request at {DateTime.Now}:\n{body}");

        // Ana Unity thread'inde işlemi yapmak için
        // Dispatcher pattern'ı burada kullanılabilir, ancak basitlik için doğrudan CubeSpawner çağrılıyor.
        // Eğer thread güvenliği sorun olursa, MainThreadDispatcher gibi bir kütüphane kullanılabilir.
        // Şimdilik test amaçlı doğrudan çağrı yapıyoruz.
        // Daha güvenli bir yöntem: UnityMainThreadDispatcher gibi bir kütüphane kullanmak.
        // Ancak şu anki basitlik için aşağıdakini kullanabiliriz.

        // UI ve GameObject işlemleri ana thread'de yapılmalı.
        // Bu kısım doğrudan HttpListener thread'inde çalışır, bu yüzden Unity API'larını doğrudan çağırmak sorun yaratabilir.
        // Bu sorunu çözmek için bir queue sistemi veya UnityMainThreadDispatcher gibi bir kütüphane kullanmalıyız.
        // Şimdilik, Hata almaman için basit bir örnek bırakıyorum, ancak bu "doğru" yol değildir:

        // Eğer JSON'ı başarılı bir şekilde okuduysak, CubeSpawner'a gönder
        // Bu kısım için MainThreadDispatcher gibi bir yapı KESİNLİKLE önerilir.
        // Basitlik için _cubeSpawner.OnJSONReceived(body); diyebiliriz ama bu thread-safe değil.

        // Geçici çözüm (doğru çözüm değil, sadece hızlıca test etmek için):
        // Unity API'lerine başka bir thread'den erişmek güvenlik ihlali ve crash'e yol açabilir.
        // Normalde bunu bir kuyruğa atıp Update() içinde işlemek lazım.
        //UnityMainThreadDispatcher.Instance().Enqueue(() => _cubeSpawner.OnJSONReceived(body));
        //_cubeSpawner.OnJSONReceived(body); // GEÇİCİ ÇÖZÜM: Thread güvenli DEĞİL!
        // ÖNCEKİ SATIRI KALDIRDIKTAN SONRA BU YENİ SATIRI EKLE
        MainThreadDispatcher.Instance().Enqueue(() => _cubeSpawner.OnJSONReceived(body));
        // Basitçe 200 OK dönecek
        context.Response.StatusCode = 200;
        var respBytes = Encoding.UTF8.GetBytes("OK");
        context.Response.OutputStream.Write(respBytes, 0, respBytes.Length);
        context.Response.Close();

        // Sonraki isteği dinlemeye devam et
        _listener.BeginGetContext(OnRequest, null);
    }

    void OnApplicationQuit()
    {
        if (_listener != null && _listener.IsListening)
        {
            _listener.Stop();
            _listener.Close();
            Debug.Log("Unity HTTP Listener stopped.");
        }
    }
}