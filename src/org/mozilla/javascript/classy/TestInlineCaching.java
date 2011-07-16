package org.mozilla.javascript.classy;

import org.mozilla.javascript.ScriptableObject;

import java.util.HashMap;
import java.util.Map;


public class TestInlineCaching extends ClassyScriptable{
	
	
	static TestInlineCaching t0 = null,t1=null, t2=null, test=null;
	static Map<String,String> hm;

        static MonomorphicInlineCache getx = new MonomorphicInlineCache();
        static MonomorphicInlineCache gety = new MonomorphicInlineCache();

    Object x = "x";
    Object y = "y";

    public Object getX() {
        return x;
    }

    public Object getY() {
        return y;
    }

	static{

		t0 = new TestInlineCaching();
		t0.put("x", t0, "x");
		t0.put("y", t0, "y");
		t0.put("z", t0, "z");
		
		test = new TestInlineCaching();
		test.put("x", test, "x");
		test.put("y", test, "y");
		test.put("z", test, "z");
		
		t1 = new TestInlineCaching();
		t1.put("name", t1, "rohit");
		t1.put("x", t1, "x");
		t1.put("y", t1, "y");
		
		t2 = new TestInlineCaching();
		t2.put("name", t2, "hannes");
		t2.put("x", t2, "x");
		t2.put("y", t2, "y");
		
		hm = new HashMap<String, String>();
		hm.put("x", "x");
		hm.put("y", "y");
		
	}
	

	public static void main(String[] args) {
		
		getXY(t0);
		getXY(test);
		getXY(t1);
		getXY(t2);

		getXYWithInlineCaching(t0);
		getXYWithInlineCaching(test);
		getXYWithInlineCaching(t1);
		getXYWithInlineCaching(t2);
		
		getXYWithHashMap(hm);


		long time;

		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			getXY(t0);
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		System.out.println("");
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			getXYWithInlineCaching(t0);
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		

		System.out.println();
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			getXYWithHashMap(hm);
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		System.out.println();

		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			getXYFromField(t0);
			System.out.print(System.currentTimeMillis() - time+"\t");
		}

		System.out.println();
	}
	
	private static void getXY(ScriptableObject tic){
            for(int j=0;j<10000000;j++){
                tic.get("x", tic);
                tic.get("y", tic);
                tic.get("x", tic);
                tic.get("y", tic);
                tic.get("x", tic);
                tic.get("y", tic);
                tic.get("x", tic);
                tic.get("y", tic);
                tic.get("x", tic);
                tic.get("y", tic);
            }
	}
	
	private static void getXYWithInlineCaching(TestInlineCaching tic){
            for(int j=0;j<10000000;j++){
                getx.get(tic, "x");
                gety.get(tic, "y");
                getx.get(tic, "x");
                gety.get(tic, "y");
                getx.get(tic, "x");
                gety.get(tic, "y");
                getx.get(tic, "x");
                gety.get(tic, "y");
                getx.get(tic, "x");
                gety.get(tic, "y");
            }
	}
	
	private static void getXYWithHashMap(Map<String,String> hmap){
            for(int j=0;j<10000000;j++){
                hmap.get("x");
                hmap.get("y");
                hmap.get("x");
                hmap.get("y");
                hmap.get("x");
                hmap.get("y");
                hmap.get("x");
                hmap.get("y");
                hmap.get("x");
                hmap.get("y");
            }
	}

    private static void getXYFromField(TestInlineCaching tic) {
        for(int j=0;j<10000000;j++){
            tic.getX();
            tic.getY();
            tic.getX();
            tic.getY();
            tic.getX();
            tic.getY();
            tic.getX();
            tic.getY();
            tic.getX();
            tic.getY();
        }
    }

}

class MonomorphicInlineCache {
    ClassyLayout type = null;
    int offset;

    public Object get(ClassyScriptable cs, String key) {
        if (type == null) {
            type = cs.getLayout();
            offset = type.findMapping(key).offset();
        }
        return cs.getValueAtOffset(offset, cs);
    }

}
