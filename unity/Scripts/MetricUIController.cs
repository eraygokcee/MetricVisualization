using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using TMPro;
using UnityEngine.UI;

public class MetricUIController : MonoBehaviour
{
    [Header("UI Referanslarý")]
    public GameObject panel; //
    public TMP_Dropdown heightDropdown; //
    public TMP_Dropdown widthDropdown; //
    public TMP_Dropdown colorDropdown; //
    public Button confirmButton; //

    [Header("Dinamik Aralýk Girdileri")]
    public TMP_InputField heightMinInput;
    public TMP_InputField heightMaxInput;
    public TMP_InputField widthMinInput;
    public TMP_InputField widthMaxInput;

    private MetricMapper mapper; //
    private CubeSpawner spawner; //
    private List<MetricType> availableMetrics = new List<MetricType>(); //

    // MetricMapper'daki varsayýlanlarý hýzlý eriþim için bir sözlüðe dönüþtüreceðiz
    private Dictionary<MetricType, MetricRange> _defaultRangeMap;

    void Start()
    {
        mapper = FindObjectOfType<MetricMapper>(); //
        spawner = FindObjectOfType<CubeSpawner>(); //

        if (mapper == null || spawner == null)
        {
            Debug.LogError("Mapper veya Spawner bulunamadý!");
            return;
        }

        // Mapper'daki Inspector ayarlarýný bir sözlüðe çevir.
        _defaultRangeMap = mapper.metricRanges.ToDictionary(r => r.metricType, r => r);

        panel.SetActive(false); //
        confirmButton.onClick.AddListener(OnConfirm); //

        // Dropdown deðeri her deðiþtiðinde, ilgili input alanlarýný güncelle.
        heightDropdown.onValueChanged.AddListener(delegate { UpdateInputsFromDropdown(heightDropdown, heightMinInput, heightMaxInput); });
        widthDropdown.onValueChanged.AddListener(delegate { UpdateInputsFromDropdown(widthDropdown, widthMinInput, widthMaxInput); });
    }

    /// <summary>
    /// Paneli gösterir ve dropdownlarý doldurur.
    /// </summary>
    public void Show()
    {
        panel.SetActive(true); //
        PopulateDropdowns(); //
    }

    void PopulateDropdowns()
    {
        // ... (Bu metodun içeriði ayný kalabilir, sadece mevcut metrikleri dropdown'a ekler) ...
        var options = availableMetrics.Select(m => new TMP_Dropdown.OptionData(m.ToString())).ToList();

        heightDropdown.ClearOptions();
        widthDropdown.ClearOptions();
        colorDropdown.ClearOptions();

        heightDropdown.AddOptions(options);
        widthDropdown.AddOptions(options);
        colorDropdown.AddOptions(options);

        // Mevcut mapper deðerlerine göre varsayýlanlarý ayarla ve input'larý tetikle.
        heightDropdown.value = availableMetrics.IndexOf(mapper.heightMetric);
        widthDropdown.value = availableMetrics.IndexOf(mapper.widthMetric);
        colorDropdown.value = availableMetrics.IndexOf(mapper.colorMetric);

        // Panelin ilk açýlýþýnda inputlarý doldur.
        UpdateInputsFromDropdown(heightDropdown, heightMinInput, heightMaxInput);
        UpdateInputsFromDropdown(widthDropdown, widthMinInput, widthMaxInput);
    }

    /// <summary>
    /// Bir dropdown'daki seçime göre ilgili input alanlarýný varsayýlan deðerlerle doldurur.
    /// </summary>
    private void UpdateInputsFromDropdown(TMP_Dropdown dropdown, TMP_InputField minInput, TMP_InputField maxInput)
    {
        string selectedMetricName = dropdown.options[dropdown.value].text;
        if (Enum.TryParse<MetricType>(selectedMetricName, out var metricType))
        {
            if (_defaultRangeMap.TryGetValue(metricType, out var range))
            {
                minInput.text = range.minVal.ToString();
                maxInput.text = range.maxVal.ToString();
            }
        }
    }

    public void SetAvailableMetrics(IEnumerable<string> metricNames)
    {
        // ... (Bu metodun içeriði ayný kalabilir) ...
        availableMetrics.Clear();
        foreach (var name in metricNames)
        {
            if (Enum.TryParse<MetricType>(name, true, out var mt) && mt != MetricType.None)
            {
                availableMetrics.Add(mt);
            }
        }
    }

    void OnConfirm()
    {
        // 1. Dropdown'dan seçilen metrikleri al
        MetricType heightMet = (MetricType)Enum.Parse(typeof(MetricType), heightDropdown.options[heightDropdown.value].text);
        MetricType widthMet = (MetricType)Enum.Parse(typeof(MetricType), widthDropdown.options[widthDropdown.value].text);
        MetricType colorMet = (MetricType)Enum.Parse(typeof(MetricType), colorDropdown.options[colorDropdown.value].text);

        // 2. Input alanlarýndaki DEÐÝÞTÝRÝLMÝÞ OLABÝLECEK deðerleri oku
        float hMin = float.Parse(heightMinInput.text);
        float hMax = float.Parse(heightMaxInput.text);
        float wMin = float.Parse(widthMinInput.text);
        float wMax = float.Parse(widthMaxInput.text);

        // 3. Spawner'ý çalýþtýrmadan ÖNCE, MetricMapper'daki deðerleri bu son deðerlerle güncelle
        mapper.heightMetric = heightMet;
        mapper.widthMetric = widthMet;
        mapper.colorMetric = colorMet;

        // MetricRange'leri de güncelle. Bunun için MetricMapper'a yeni bir metot eklemek en temizi olur.
        // Ama þimdilik doðrudan deðiþtirebiliriz.
        var heightRange = _defaultRangeMap[heightMet];
        heightRange.minVal = hMin;
        heightRange.maxVal = hMax;

        var widthRange = _defaultRangeMap[widthMet];
        widthRange.minVal = wMin;
        widthRange.maxVal = wMax;

        // Bu deðiþikliklerin kalýcý olmamasý için mapper'daki asýl diziyi deðil,
        // spawn iþleminde kullanýlacak geçici deðerleri güncellemek daha iyi olabilir.
        // Ancak basitlik için þimdilik bu þekilde býrakabiliriz.

        panel.SetActive(false); //
        spawner.DoSpawn(); //
    }
}