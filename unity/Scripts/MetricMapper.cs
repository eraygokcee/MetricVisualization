using System;
using System.Linq;
using UnityEngine;

public enum MetricType { None, WMC, DIT, LCOM, TCC, CBO , MAXCYCLO, AVGCYCLO }

[Serializable]
public class MetricRange
{
    public MetricType metricType;
    public float minVal, maxVal;
    public bool isLowerBetter;
}

public class MetricMapper : MonoBehaviour
{
    [Header("Hangi metrik hangi özelliğe?")]
    public MetricType heightMetric = MetricType.WMC;
    public MetricType widthMetric = MetricType.LCOM;
    public MetricType colorMetric = MetricType.TCC;

    [Header("Normalize Aralıkları (height/width için)")]
    public MetricRange[] metricRanges;
    public float minHeight = 1f, maxHeight = 20f;
    public float minWidth = 1f, maxWidth = 10f;

    /// <summary>Raw metrik değerini döner.</summary>
    public float GetMetricValue(MetricValues m, MetricType type)
    {
        switch (type)
        {
            case MetricType.WMC: return m.wmc;
            case MetricType.DIT: return m.dit;
            case MetricType.LCOM: return m.lcom;
            case MetricType.TCC: return m.tcc;
            case MetricType.CBO: return m.cbo;
            case MetricType.MAXCYCLO: return m.maxcyclo;
            case MetricType.AVGCYCLO: return m.avgcyclo;
            default: return 0;
        }
    }

    /// <summary>0–1 arası normalize edilmiş değer (height/width için).</summary>
    public float Normalize(float val, MetricType type)
    {
        var range = metricRanges.FirstOrDefault(r => r.metricType == type);
        if (range == null)
        {
            Debug.LogWarning($"MetricRange for {type} not found. Returning 0.");
            return 0;
        }
        float t = Mathf.InverseLerp(range.minVal, range.maxVal, val);
        return range.isLowerBetter ? 1 - t : t;
    }

    /// <summary>
    /// Yeni overload: raw değer + metricType’a göre direkt renk döner.
    /// </summary>
    public Color GetColor(MetricType type, float rawVal)
    {
        float t;
        switch (type)
        {
            case MetricType.CBO:
                // 0 (green) → 12 (red)
                t = Mathf.InverseLerp(0f, 12f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            case MetricType.TCC:
                // 0 (red) → 1 (green)
                t = Mathf.InverseLerp(0f, 1f, rawVal);
                return Color.Lerp(Color.red, Color.green, t);

            case MetricType.WMC:
                // 0 (green) → 10 (red)
                t = Mathf.InverseLerp(0f, 30f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            case MetricType.LCOM:
                // 0 (green) → 5 (red)
                t = Mathf.InverseLerp(0f, 5f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            case MetricType.DIT:
                // 0 (green) → 5 (red)
                t = Mathf.InverseLerp(0f, 5f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            case MetricType.MAXCYCLO:
                t = Mathf.InverseLerp(1f, 20f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            case MetricType.AVGCYCLO:
                t = Mathf.InverseLerp(1f, 7f, rawVal);
                return Color.Lerp(Color.green, Color.red, t);

            default:
                return Color.gray;
        }
    }
}
