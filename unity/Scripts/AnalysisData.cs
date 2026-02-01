using UnityEngine;
using System;

[Serializable]
public class AnalysisData
{
    public string id;
    public string type;
    public string targetPath;
    public string projectName;
    public ClassMetrics[] results;
}
