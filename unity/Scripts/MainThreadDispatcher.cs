using System;
using System.Collections.Generic;
using UnityEngine;

public class MainThreadDispatcher : MonoBehaviour
{
    private static readonly Queue<Action> _executionQueue = new Queue<Action>();
    private static MainThreadDispatcher _instance;

    // Singleton deseni: Tek bir instance olmasýný saðlar
    public static MainThreadDispatcher Instance()
    {
        if (_instance == null)
        {
            // Eðer sahnede yoksa, otomatik olarak yeni bir GameObject oluþtur ve ona bu scripti ekle
            GameObject go = new GameObject("MainThreadDispatcher");
            _instance = go.AddComponent<MainThreadDispatcher>();
        }
        return _instance;
    }

    void Awake()
    {
        if (_instance == null)
        {
            _instance = this;
            DontDestroyOnLoad(gameObject); // Sahneler arasý geçiþte yok olmamasýný saðlar (isteðe baðlý)
        }
        else if (_instance != this)
        {
            Destroy(gameObject); // Zaten bir instance varsa kendini yok et
        }
    }

    void Update()
    {
        // Ana thread'de kuyruktaki tüm iþlemleri sýrayla çalýþtýr
        lock (_executionQueue) // Kuyruða ayný anda eriþimi engellemek için kilitle
        {
            while (_executionQueue.Count > 0)
            {
                _executionQueue.Dequeue().Invoke();
            }
        }
    }

    /// <summary>
    /// Arka plan thread'inden ana thread'de çalýþtýrýlacak bir görevi kuyruða ekler.
    /// </summary>
    public void Enqueue(Action action)
    {
        lock (_executionQueue) // Kuyruða ayný anda eriþimi engellemek için kilitle
        {
            _executionQueue.Enqueue(action);
        }
    }
}