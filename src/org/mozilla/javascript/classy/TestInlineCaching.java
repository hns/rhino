package org.mozilla.javascript.classy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestInlineCaching extends ClassyScriptable{
	
	
	static TestInlineCaching t0 = null,t1=null, t2=null, test=null;
	static Map<String,Integer> hm;
	static ClassyLayout possibleType = null;
	static int possibleOffset;
	
	static{

		t0 = new TestInlineCaching();
		t0.put("x", null, 1);
		t0.put("y", null, 2);
		t0.put("z", null, 3);
		
		test = new TestInlineCaching();
		test.put("x", null, 4);
		test.put("y", null, 5);
		test.put("z", null, 6);
		
		t1 = new TestInlineCaching();
		t1.put("name", null, "rohit");
		t1.put("x", null, 10);
		t1.put("y", null, 20);
		
		t2 = new TestInlineCaching();
		t2.put("name", null, "hannes");
		t2.put("x", null, 100);
		t2.put("y", null, 200);
		
		hm = new HashMap<String, Integer>();
		hm.put("x",10);
		hm.put("y",20);
		
	}
	

	public static void main(String[] args) {
		
		System.out.println(lengthSquared(t0));
		System.out.println(lengthSquared(test));
		System.out.println(lengthSquared(t1));
		System.out.println(lengthSquared(t2));

		System.out.println(lengthSquaredWithInlineCaching(t0));
		lengthSquaredWithInlineCaching(t0);
		System.out.println(lengthSquaredWithInlineCaching(test));
		System.out.println(lengthSquaredWithInlineCaching(t1));
		System.out.println(lengthSquaredWithInlineCaching(t2));
		
		System.out.println(lengthSquaredWithHashMap(hm));


		long time = 0;

		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				lengthSquared(t0);
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		System.out.println("");
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				lengthSquaredWithInlineCaching(t0);
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		

		System.out.println();
		
		for(int  i=0;i<10;i++){
			time = System.currentTimeMillis();
			for(int j=0;j<10000000;j++){
				lengthSquaredWithHashMap(hm);
			}
			System.out.print(System.currentTimeMillis() - time+"\t");
		}
		
		
		
	}


	private Object getWithInlineCaching(String key) {

		if(possibleType == null){
			Map<String,Object> typeInformatioMap = this.extendedGet(key, null);
			possibleType = (ClassyLayout)typeInformatioMap.get("type");
			possibleOffset = (Integer)typeInformatioMap.get("offset");
			return typeInformatioMap.get("value");
		}else{
			ClassyLayout cuurentType = this.getTypeOfObject();
			if(cuurentType == possibleType){
				possibleOffset = this.getoffset(key);
				return this.getValueAtOffset(possibleOffset);
			}else{
				possibleType = null;
				return getWithInlineCaching(key);
			}
		}
	
	}
	
	private static int lengthSquared(TestInlineCaching tic){
		return (Integer)tic.get("x") * (Integer) tic.get("y");
	}
	
	private static int lengthSquaredWithInlineCaching(TestInlineCaching tic){
		return (Integer)tic.getWithInlineCaching("x") * (Integer) tic.getWithInlineCaching("y");
	}
	
	private static int lengthSquaredWithHashMap(Map<String,Integer> hmap){
		return (Integer)hmap.get("x") * (Integer)hmap.get("y");
	}


}
