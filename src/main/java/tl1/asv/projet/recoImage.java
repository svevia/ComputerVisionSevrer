package tl1.asv.projet;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_features2d.drawKeypoints;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_core.DMatch;
import org.bytedeco.javacpp.opencv_core.DMatchVector;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_features2d.DrawMatchesFlags;
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT;

public class recoImage {
	
	static DMatchVector	selectBest(DMatchVector	matches,	int	numberToSelect)	{
		DMatch[] sorted	= toArray(matches);	
		Arrays.sort(sorted,	(a,	b)	->	{
			return	a.lessThan(b)	?	-1	:	1;
		});
		DMatch[] best	= Arrays.copyOf(sorted,	numberToSelect);
		return	new	DMatchVector(best);
	}
	
	static DMatch[]	toArray(DMatchVector	matches)	{
		assert	matches.size()	<=	Integer.MAX_VALUE;
		int	n	=	(int)	matches.size();
		//	Convert	keyPoints	to	Scala	sequence
		DMatch[]	result	=	new	DMatch[n];
		for	(int	i	=	0; i	<	n;	i++)	{
			result[i]	=	new	DMatch(matches.get(i));
		}
		return	result;
	}
	
	
	static float getDist(Mat img1, Mat img2){
		int nFeatures =	0;
		int nOctaveLayers =	3;
		double contrastThreshold =	0.03;
		int edgeThreshold =	10;
		double sigma =	1.6;
		Loader.load(opencv_calib3d.class);
		Loader.load(opencv_shape.class)	;

		KeyPointVector kpv1 = new KeyPointVector();
		KeyPointVector kpv2 = new KeyPointVector();

		SIFT	sift;
		sift =	SIFT.create(nFeatures,	nOctaveLayers,	contrastThreshold,	edgeThreshold,	sigma);
		sift.detect(img1, kpv1);
		sift.detect(img2, kpv2);
		
		Mat feature1 = new Mat();
		Mat feature2 = new Mat();

		drawKeypoints(img1,	kpv1,	feature1,	new Scalar(255,	255,	255,	0),	DrawMatchesFlags.DRAW_RICH_KEYPOINTS);
		drawKeypoints(img2,	kpv2,	feature2,	new Scalar(255,	255,	255,	0),	DrawMatchesFlags.DRAW_RICH_KEYPOINTS);

		Mat desc1 = new Mat();
		Mat desc2 = new Mat();
		
		sift.compute(img1, kpv1, desc1);
		sift.compute(img2, kpv2, desc2);
		
		BFMatcher matcher =	new BFMatcher(NORM_L2,	false);
		DMatchVector match = new DMatchVector();
		matcher.match(desc1,	desc2,	match);
		
		DMatchVector bestMatches = selectBest(match,25);
		float moy = 0;
		for(int j = 0; j < bestMatches.size();j++){
			moy += bestMatches.get(j).distance();
		}
		moy = moy/bestMatches.size();
		return moy;
		
	}

	
	
    static Map<String, Float> sortByComparator(Map<String, Float> unsortMap, final boolean order)
    {

        List<Entry<String, Float>> list = new LinkedList<Entry<String, Float>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Float>>()
        {
            public int compare(Entry<String, Float> o1,
                    Entry<String, Float> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        for (Entry<String, Float> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
