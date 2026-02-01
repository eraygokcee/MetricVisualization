using System;

using System.Collections.Generic;

using System.Linq;

using UnityEngine;



public class CubeSpawner : MonoBehaviour

{

    [HideInInspector] public AnalysisData parsedData;



    [Header("Görsel Referanslar")]

    public GameObject buildingCubePrefab;

    public Transform cityParentTransform;



    [Header("Grid Ayarlarý")]

    public float spacing = 50f;

    [Tooltip("Küpler arasýnda býrakýlacak ek boþluk")]

    public float margin = 2f;



    [Header("Terrain Ayarlarý")]

    [Tooltip("Þehir alanýnýn kenarýndan itibaren daðlarýn baþlayacaðý mesafe.")]

    public float terrainMargin = 200f;

    [Tooltip("Terrain'in maksimum dünya yüksekliði.")]

    public float terrainHeightScale = 250f;

    [Tooltip("Düz þehir alanýnýn yüksekliði (0-1 arasýnda normalize).")]

    [Range(0f, 1f)]

    public float flatAreaHeight = 0.1f;



    [Header("Terrain Gürültü Ayarlarý")]

    [Tooltip("Daðlarýn oluþumunda kullanýlacak gürültü frekansý.")]

    public float noiseFrequency = 0.03f;

    [Tooltip("Heightmap çözünürlüðü (Örn: 129, 257, 513).")]

    public int heightmapResolution = 513;



    private string groundTextureName = "Ground004"; // Örn: Vol_42_1_Base_Color

    private string mountainTextureName = "Ground001";



    private Terrain generatedTerrain;



    private MetricMapper mapper;

    private MetricUIController uiController;



    void Awake()

    {

        mapper = GetComponent<MetricMapper>();

        uiController = FindObjectOfType<MetricUIController>();



        if (cityParentTransform == null)

        {

            var cp = GameObject.Find("CityParent");

            if (cp != null) cityParentTransform = cp.transform;

            else Debug.LogError("CityParent GameObject bulunamadý!");

        }

    }



    public void OnJSONReceived(string jsonString)

    {

        parsedData = JsonUtility.FromJson<AnalysisData>(jsonString);



        int start = jsonString.IndexOf("\"metrics\"");

        string metricsSection = "";

        if (start >= 0)

        {

            int braceOpen = jsonString.IndexOf('{', start);

            int braceClose = jsonString.IndexOf('}', braceOpen);

            if (braceOpen >= 0 && braceClose > braceOpen)

                metricsSection = jsonString

                  .Substring(braceOpen, braceClose - braceOpen + 1)

                  .ToLowerInvariant();

        }



        var metricNames = new List<string>();

        foreach (var field in typeof(MetricValues).GetFields())

        {

            string key = $"\"{field.Name.ToLowerInvariant()}\"";

            bool presentInJson = metricsSection.Contains(key);



            bool nonZero = parsedData.results.Any(cm =>

            {

                var val = field.GetValue(cm.metrics);

                if (val is int iv) return iv != 0;

                if (val is float fv) return !Mathf.Approximately(fv, 0f);

                return false;

            });



            if (presentInJson || nonZero)

                metricNames.Add(field.Name);

        }



        uiController.SetAvailableMetrics(metricNames);

        uiController.Show();

    }



    public void DoSpawn()

    {

        if (parsedData == null || parsedData.results == null || parsedData.results.Length == 0)

        {

            Debug.LogWarning("Görselleþtirilecek veri yok veya boþ.");

            return;

        }



        foreach (Transform child in cityParentTransform)

            Destroy(child.gameObject);



        if (generatedTerrain != null)

            Destroy(generatedTerrain.gameObject);



        var list = parsedData.results;

        int gridSize = Mathf.Max(1, Mathf.CeilToInt(Mathf.Sqrt(list.Length)));



        float maxCubeWidth = 0f;

        if (list.Length > 0)

        {

            maxCubeWidth = list

              .Select(cm => {

                  float wRaw = mapper.GetMetricValue(cm.metrics, mapper.widthMetric);

                  var rW = mapper.metricRanges.First(r => r.metricType == mapper.widthMetric);

                  float wN = Mathf.InverseLerp(rW.minVal, rW.maxVal, wRaw);

                  return Mathf.Lerp(mapper.minWidth, mapper.maxWidth, wN);

              })

              .Max();

        }



        float usedSpacing = Mathf.Max(spacing, maxCubeWidth + margin);

        float cityAreaSize = (gridSize > 0 ? (gridSize - 1) * usedSpacing : 0) + maxCubeWidth;



        GenerateCityTerrain(cityAreaSize);



        Vector3 cityGridOrigin = new Vector3(-cityAreaSize / 2f, flatAreaHeight * terrainHeightScale, -cityAreaSize / 2f);



        for (int i = 0; i < list.Length; i++)

        {

            var cm = list[i];

            float hRaw = mapper.GetMetricValue(cm.metrics, mapper.heightMetric);

            float wRaw = mapper.GetMetricValue(cm.metrics, mapper.widthMetric);

            float cRaw = mapper.GetMetricValue(cm.metrics, mapper.colorMetric);



            var rH = mapper.metricRanges.First(r => r.metricType == mapper.heightMetric);

            float hN = Mathf.InverseLerp(rH.minVal, rH.maxVal, hRaw);



            float height = Mathf.Lerp(mapper.minHeight, mapper.maxHeight, hN);

            float width = Mathf.Lerp(mapper.minWidth, mapper.maxWidth, Mathf.InverseLerp(

              mapper.metricRanges.First(r => r.metricType == mapper.widthMetric).minVal,

              mapper.metricRanges.First(r => r.metricType == mapper.widthMetric).maxVal,

              wRaw));



            Vector3 pos = cityGridOrigin + new Vector3(

              (i % gridSize) * usedSpacing,

              height / 2f,

              (i / gridSize) * usedSpacing

            );



            GameObject cube = Instantiate(buildingCubePrefab, pos, Quaternion.identity, cityParentTransform);

            cube.transform.localScale = new Vector3(width, height, width);

            cube.GetComponent<Renderer>().material.color = mapper.GetColor(mapper.colorMetric, cRaw);



            var info = cube.AddComponent<BuildingInfoDisplay>();

            info.className = cm.className;

            info.metrics = cm.metrics;

        }

    }



    private void GenerateCityTerrain(float citySize)

    {

        float fullTerrainSize = citySize + terrainMargin * 2f;



        var td = new TerrainData

        {

            heightmapResolution = heightmapResolution,

            size = new Vector3(fullTerrainSize, terrainHeightScale, fullTerrainSize)

        };



        float[,] heights = GenerateHeights(citySize, fullTerrainSize);

        td.SetHeights(0, 0, heights);



        td.terrainLayers = CreateTerrainLayers();

        // DÜZELTME 1: td.alphamapWidth deðerini GenerateAlphamaps'e parametre olarak gönderiyoruz.

        float[,,] alphamaps = GenerateAlphamaps(citySize, fullTerrainSize, td.alphamapWidth);

        td.SetAlphamaps(0, 0, alphamaps);



        var terrainGO = Terrain.CreateTerrainGameObject(td);

        terrainGO.transform.SetParent(cityParentTransform);

        terrainGO.name = "GeneratedCityTerrain";



        terrainGO.transform.position = new Vector3(-fullTerrainSize / 2f, 0, -fullTerrainSize / 2f);



        generatedTerrain = terrainGO.GetComponent<Terrain>();





        // Sahnedeki CameraController'ý bul ve oluþturduðumuz terrain'i ona ata.

        var cameraController = FindObjectOfType<CameraController>();

        if (cameraController != null)

        {

            cameraController.targetTerrain = generatedTerrain;

            Debug.Log("Terrain referansý CameraController'a baþarýyla atandý.");

        }

        else

        {

            Debug.LogWarning("Sahnede CameraController bulunamadý. Terrain çarpýþma tespiti çalýþmayacak.");

        }

    }



    private float[,] GenerateHeights(float citySize, float totalSize)

    {

        int res = heightmapResolution;

        var heights = new float[res, res];

        float cityRadius = citySize / 2f;



        for (int y = 0; y < res; y++)

        {

            for (int x = 0; x < res; x++)

            {

                float worldX = ((float)x / (res - 1)) * totalSize - totalSize / 2f;

                float worldY = ((float)y / (res - 1)) * totalSize - totalSize / 2f;



                float distFromCityEdge = Mathf.Max(Mathf.Abs(worldX) - cityRadius, Mathf.Abs(worldY) - cityRadius);

                distFromCityEdge = Mathf.Max(0, distFromCityEdge);



                if (distFromCityEdge <= 0)

                {

                    heights[y, x] = flatAreaHeight;

                }

                else

                {

                    float falloff = Mathf.Clamp01(distFromCityEdge / terrainMargin);

                    falloff = falloff * falloff;



                    float noise = Mathf.PerlinNoise(

                      (worldX + 1000) * noiseFrequency,

                      (worldY + 1000) * noiseFrequency

                    );



                    heights[y, x] = Mathf.Lerp(flatAreaHeight, noise, falloff);

                }

            }

        }

        return heights;

    }



    // DÜZELTME 2: Metot tanýmýný, int tipinde bir parametre alacak þekilde güncelliyoruz.

    private float[,,] GenerateAlphamaps(float citySize, float totalSize, int alphamapResolution)

    {

        // DÜZELTME 3: Hata veren satýrý, artýk parametreden gelen deðeri kullanacak þekilde deðiþtiriyoruz.

        int res = alphamapResolution;

        var alphamaps = new float[res, res, 2];

        float cityRadius = citySize / 2f;



        for (int y = 0; y < res; y++)

        {

            for (int x = 0; x < res; x++)

            {

                float worldX = ((float)x / (res - 1)) * totalSize - totalSize / 2f;

                float worldY = ((float)y / (res - 1)) * totalSize - totalSize / 2f;



                float distFromCityEdge = Mathf.Max(Mathf.Abs(worldX) - cityRadius, Mathf.Abs(worldY) - cityRadius);

                distFromCityEdge = Mathf.Max(0, distFromCityEdge);



                float mountainWeight = Mathf.Clamp01(distFromCityEdge / terrainMargin);

                mountainWeight = Mathf.SmoothStep(0, 1, mountainWeight);



                alphamaps[y, x, 0] = 1 - mountainWeight;

                alphamaps[y, x, 1] = mountainWeight;

            }

        }

        return alphamaps;

    }



    private TerrainLayer[] CreateTerrainLayers()

    {

        // Dokularý Resources klasöründen isimleriyle yükle

        Texture2D groundTex = Resources.Load<Texture2D>(groundTextureName);

        Texture2D mountainTex = Resources.Load<Texture2D>(mountainTextureName);



        // Yüklenip yüklenmediklerini kontrol et (Hata ayýklama için önemli)

        if (groundTex == null)

        {

            Debug.LogError($"HATA: '{groundTextureName}' adýnda bir doku Assets/Resources klasöründe bulunamadý!");

        }

        if (mountainTex == null)

        {

            Debug.LogError($"HATA: '{mountainTextureName}' adýnda bir doku Assets/Resources klasöründe bulunamadý!");

        }



        return new TerrainLayer[]

        {

      new TerrainLayer { diffuseTexture = groundTex, tileSize = new Vector2(20, 20) },

      new TerrainLayer { diffuseTexture = mountainTex, tileSize = new Vector2(50, 50) }

        };

    }

}

