using UnityEngine;
using TMPro;
using System.Text;

/// <summary>
/// Fareyle üzerine gelindiðinde yalnýzca kullanýcý seçimine göre gösterilen metrikleri,
/// deðerleri sýfýr olsa bile listeler.
/// </summary>
public class BuildingInfoDisplay : MonoBehaviour
{
    public string className;
    public MetricValues metrics;

    private static TMP_Text uiText;
    private MetricMapper mapper;

    void Start()
    {
        // InfoText alanýný bul
        if (uiText == null)
        {
            GameObject textObj = GameObject.Find("Canvas/InfoPanel/InfoText");
            if (textObj != null)
                uiText = textObj.GetComponent<TMP_Text>();
            else
                Debug.LogError("InfoText bulunamadý! Yol: Canvas/InfoPanel/InfoText");
        }

        // Mapper'ý yakala
        mapper = FindObjectOfType<MetricMapper>();
        if (mapper == null)
            Debug.LogError("MetricMapper bulunamadý!");
    }

    void OnMouseEnter()
    {
        if (uiText == null || mapper == null)
            return;

        var sb = new StringBuilder();
        sb.AppendLine($"<b>{className}</b>");

        // Kullanýcýnýn UI'de seçtiði 3 metrik
        var selectedMetrics = new[]
        {
            mapper.heightMetric,
            mapper.widthMetric,
            mapper.colorMetric
        };

        foreach (var metric in selectedMetrics)
        {
            // MetricValues içindeki alan adýný bul
            var field = typeof(MetricValues).GetField(metric.ToString().ToLowerInvariant());
            if (field == null)
                continue;

            var val = field.GetValue(metrics);
            string formatted = val is float f ? f.ToString("F2") : val.ToString();

            sb.AppendLine($"{metric}: {formatted}");
        }

        uiText.text = sb.ToString();
    }

    void OnMouseExit()
    {
        if (uiText != null)
            uiText.text = string.Empty;
    }
}
