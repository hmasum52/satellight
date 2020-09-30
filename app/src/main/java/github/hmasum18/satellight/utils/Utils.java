package github.hmasum18.satellight.utils;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Utils {

    public static Map<String,String> sscSatCodeMap = new HashMap<String, String>(){{
        put("ISS","iss");
        put("NOAA-19","noaa19");
        put("Aqua","aqua");
        put("GOES-13","goes13");
        put("CASSIOPE","cassiope");
        put("MOON","moon");
    }};


    /**
     * @param timestamp is in milli second
     * @return timestamp in form ISO 8601 format
     */
    public static String getTimeAsString(long timestamp ){
        timestamp /=(1000*60);
        timestamp *= (1000*60); ///exclude the extra milli sec time

        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcDate = utcFormat.format(timestamp);

        return utcDate;
    }

    /**
     * @param timestamp is in milli second
     * @return timestamp in form ISO 8601 format
     */
    public static String getTimeAsStringForPostReq(long timestamp ){
        timestamp /=(1000*60);
        timestamp *= (1000*60); ///exclude the extra milli sec time

        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmmZ");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcDate = utcFormat.format(timestamp);

        return utcDate;
    }

    public static  String sscRequestJSONString = "{\n" +
            "    \"BfieldModel\": {\n" +
            "        \"ExternalBFieldModel\": [\n" +
            "            \"gov.nasa.gsfc.sscweb.schema.Tsyganenko89CBFieldModel\",\n" +
            "            {\n" +
            "                \"KeyParameterValues\": \"KP_3_3_3\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"InternalBFieldModel\": \"IGRF\",\n" +
            "        \"TraceStopAltitude\": 100\n" +
            "    },\n" +
            "    \"Description\": \"Complex locator request with nearly all options.\",\n" +
            "    \"OutputOptions\": {\n" +
            "        \"AllLocationFilters\": true,\n" +
            "        \"CoordinateOptions\": [\n" +
            "            \"java.util.ArrayList\",\n" +
            "            [\n" +
            "                {\n" +
            "                    \"Component\": \"X\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"Component\": \"Y\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"Component\": \"Z\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"Component\": \"LAT\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"Component\": \"LON\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"Component\": \"LOCAL_TIME\",\n" +
            "                    \"CoordinateSystem\": \"GEO\"\n" +
            "                }\n" +
            "            ]\n" +
            "        ],\n" +
            "        \"DistanceFromOptions\": {\n" +
            "            \"BgseXYZ\": true,\n" +
            "            \"BowShock\": true,\n" +
            "            \"Mpause\": true,\n" +
            "            \"NeutralSheet\": true\n" +
            "        },\n" +
            "        \"MinMaxPoints\": 2,\n" +
            "        \"RegionOptions\": {\n" +
            "            \"NorthBTracedFootpoint\": true,\n" +
            "            \"RadialTracedFootpoint\": true,\n" +
            "            \"SouthBTracedFootpoint\": true,\n" +
            "            \"Spacecraft\": true\n" +
            "        },\n" +
            "        \"ValueOptions\": {\n" +
            "            \"BfieldStrength\": true,\n" +
            "            \"DipoleInvLat\": true,\n" +
            "            \"DipoleLValue\": true,\n" +
            "            \"RadialDistance\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"RegionFilterOptions\": {\n" +
            "        \"MagneticTraceRegions\": {\n" +
            "            \"AuroralOval\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"Cleft\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"Cusp\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"LowLatitude\": true,\n" +
            "            \"MidLatitude\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"PolarCap\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            }\n" +
            "        },\n" +
            "        \"RadialTraceRegions\": {\n" +
            "            \"AuroralOval\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"Cleft\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"Cusp\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"LowLatitude\": true,\n" +
            "            \"MidLatitude\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            },\n" +
            "            \"PolarCap\": {\n" +
            "                \"North\": true,\n" +
            "                \"South\": true\n" +
            "            }\n" +
            "        },\n" +
            "        \"SpaceRegions\": {\n" +
            "            \"DaysideMagnetosheath\": true,\n" +
            "            \"DaysideMagnetosphere\": true,\n" +
            "            \"DaysidePlasmasphere\": true,\n" +
            "            \"HighLatitudeBoundaryLayer\": true,\n" +
            "            \"InterplanetaryMedium\": true,\n" +
            "            \"LowLatitudeBoundaryLayer\": true,\n" +
            "            \"NightsideMagnetosheath\": true,\n" +
            "            \"NightsideMagnetosphere\": true,\n" +
            "            \"NightsidePlasmasphere\": true,\n" +
            "            \"PlasmaSheet\": true,\n" +
            "            \"TailLobe\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"Satellites\": [\n" +
            "        \"java.util.ArrayList\",\n" +
            "        [\n" +
            "            {\n" +
            "                \"Id\": \"goes13\",\n" +
            "                \"ResolutionFactor\": 1\n" +
            "            },\n" +
            "            {\n" +
            "                \"Id\": \"iss\",\n" +
            "                \"ResolutionFactor\": 1\n" +
            "            },\n" +
            "            {\n" +
            "                \"Id\": \"aqua\",\n" +
            "                \"ResolutionFactor\": 1\n" +
            "            },\n" +
            "            {\n" +
            "                \"Id\": \"cassiope\",\n" +
            "                \"ResolutionFactor\": 1\n" +
            "            },\n" +
            "            {\n" +
            "                \"Id\": \"noaa19\",\n" +
            "                \"ResolutionFactor\": 1\n" +
            "            }\n" +
            "        ]\n" +
            "    ]\n" +
            "}";
}
