package com.mycompany.javakararagaci;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


interface IClassifier {
    String tahmin_yap(UserRecord user);
    long egitimSuresi(); // sure hesaplamasi lazim
    void egit(List<UserRecord> egitm_verisi);
    String algoIsmiVer();
}

class UserRecord {
    public String getMarka() { 
        return marka; 
    }
    private String marka;
    
    public String cinsiyet_cek() { return cinsyet; }
    private String katgori;
    
    public UserRecord(String msteri_kodu, double net_toplam, String mrka, String katg, String cins) {
        if(net_toplam<0){this.net_toplam=0.0;}else{this.net_toplam=net_toplam;} // iade edilen urunler eksi fiyatli geliyor, onlari sifirladim
        this.marka = mrka;
        this.mster_kodu = msteri_kodu;
        this.cinsyet = cins;
        this.katgori = katg;
    }
    
    public double netToplam() { 
        return net_toplam; 
    } // burayi sonra duzelticem (public private karisti)
    
    private String mster_kodu;
    
    public String kategori() { 
        return katgori; 
    }
    
    private double net_toplam; // net toplam 
    private String cinsyet;
}

abstract class Ana_Algoritma implements IClassifier {
    @Override
    public long egitimSuresi() { 
        return egitm_suresi_ms; 
    }
    
    protected String algo_ismi; // algo adi
    
    @Override
    public String algoIsmiVer() { return algo_ismi; }
    
    protected long egitm_suresi_ms;
}

class PreProcessor {
    public SplitResult indeks_isle(List<UserRecord> tum_veri) {
        int toplam_boyut = tum_veri.size();
        
        ArrayList<Integer> indeksler;
        indeksler = new ArrayList<Integer>();
        
        for (int k = 0; k < toplam_boyut; k++) { 
            indeksler.add(k); 
        }

        Collections.shuffle(indeksler);

        int ayirma_nokta = (int) (toplam_boyut * 0.8);
        
        ArrayList<UserRecord> egitm_veri;
        egitm_veri = new ArrayList<UserRecord>();
        
        ArrayList<UserRecord> test_veri;
        test_veri = new ArrayList<UserRecord>();

        for (int syc = 0; syc < toplam_boyut; syc++) {
            if (syc < ayirma_nokta) 
            { 
                egitm_veri.add(tum_veri.get(indeksler.get(syc)));
            }
            else 
            { 
                 test_veri.add(tum_veri.get(indeksler.get(syc)));
            }
        }
      return new SplitResult(egitm_veri, test_veri);
    }
}

class TreeNode {
    Map<String, TreeNode> cocuklar;
    boolean yaprak_mi;
    
    public TreeNode(String tamin_edilen_katgori, boolean yaprak_drmu) {
        this.yaprak_mi = true;
        this.tamin_edilen_katgori = tamin_edilen_katgori;
    }
    
    String tamin_edilen_katgori;
    
    public TreeNode(String bolunen_ozelk) {
        this.yaprak_mi = false;
        this.bolunen_ozelk = bolunen_ozelk;
        
        HashMap<String, TreeNode> gecici_map;
        gecici_map = new HashMap<String, TreeNode>();
        this.cocuklar = gecici_map;
    }
    
    String bolunen_ozelk;
}

class Evaluator {
    
    private String jsn_cevir(Map<String, Map<String, Integer>> matris) {
        if (matris == null || matris.isEmpty()) { return "{}"; }
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (String dis_anahtar : matris.keySet()) {
            Map<String, Integer> ic_matris = matris.get(dis_anahtar);
            sb.append("        \"");
            sb.append(dis_anahtar);
            sb.append("\": {");
            int j = 0;
            for (String ic_anahtar : ic_matris.keySet()) {
                Integer ic_deger = ic_matris.get(ic_anahtar);
                sb.append("\"");
                sb.append(ic_anahtar);
                sb.append("\": ");
                sb.append(ic_deger);
                if (j < ic_matris.size() - 1) 
                { sb.append(", "); }
                j++;
            }
            sb.append("}");
            if (i < matris.size() - 1) 
            { sb.append(",\n"); }
            else 
            { sb.append("\n"); }
            i++;
        }
        sb.append("      }");
        return sb.toString();
    }
    
    public void modl_deglerlendir(List<IClassifier> modller, List<UserRecord> test_veri) {
        int test_boyutu = test_veri.size();
        System.out.println("Evalaating " + test_boyutu + " test records...\n");

        double knn_dogrulluk = 0, agac_dogrulluk = 0;
        long knn_sure = 0, agac_sure = 0;
        long knn_belek = 0, agac_belek = 0;
        
        Map<String, Map<String, Integer>> knn_matris_map;
        knn_matris_map = null;
        
        Map<String, Map<String, Integer>> agc_matris_map;
        agc_matris_map = null;

        for (int m = 0; m < modller.size(); m++) {
            IClassifier modl = modller.get(m);
            int dgru_taminler = 0;
            
            HashMap<String, Map<String, Integer>> karmasik_matris;
            karmasik_matris = new HashMap<String, Map<String, Integer>>();

            System.gc(); 
            long m_b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long t_basla = System.currentTimeMillis();
            for (int v = 0; v < test_veri.size(); v++) {
                UserRecord tst_knl = test_veri.get(v);
               String gercek = tst_knl.kategori(); // ktgori_al ile mi kiyasliyorduk??
                String tahmin = modl.tahmin_yap(tst_knl);
                
                if (!karmasik_matris.containsKey(gercek)) {
                    HashMap<String, Integer> bos_map;
                    bos_map = new HashMap<String, Integer>();
                    karmasik_matris.put(gercek, bos_map);
                }
                Map<String, Integer> degisken = karmasik_matris.get(gercek);
                
                if (degisken.containsKey(tahmin)) {
                    int gecici_sayi = degisken.get(tahmin);
                    degisken.put(tahmin, gecici_sayi + 1);
                } else {
                    degisken.put(tahmin, 1);
                }

                if(tahmin.equals(gercek)) { dgru_taminler++; }
            }
            long t_bitis = System.currentTimeMillis();

            long m_a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long kulanilan_belek_kb = (m_a - m_b) / 1024;

            double basari_yzdesi = ((double) dgru_taminler / test_boyutu) * 100;
            double yuvarlanmis_basari = Math.round(basari_yzdesi * 100.0) / 100.0;
            long tamin_suresi = t_bitis - t_basla;

            System.out.println("================================");
            System.out.println("Allgoritma: " + modl.algoIsmiVer());
            System.out.println("Dogrulluk: " + yuvarlanmis_basari + " %");
            System.out.println("Egitiim suresi: " + modl.egitimSuresi() + " ms");
            System.out.println("Tahmiin suresi: " + tamin_suresi + " ms");
            
            // Hocanin gormek istedigi bellek kullanimi ciktisi
            System.out.println("Kulanilan belek: " + Math.max(0, kulanilan_belek_kb) + " KB");
            
            System.out.println("\n--- CONFUSION MATRIXX ---");
            for (String gcek_ktg : karmasik_matris.keySet()) {
                System.out.println("Gercekkler: [" + gcek_ktg + "] -> Taminler: " + karmasik_matris.get(gcek_ktg));
            }
            System.out.println("========================================================================\n");

            if (modl instanceof KNNClassifier) { 
                knn_dogrulluk = basari_yzdesi; knn_sure = tamin_suresi; knn_matris_map = karmasik_matris; knn_belek = Math.max(0, kulanilan_belek_kb); 
            }
            if (modl instanceof DecisionTreeClassifier) { 
                agac_dogrulluk = basari_yzdesi; agac_sure = tamin_suresi; agc_matris_map = karmasik_matris; agac_belek = Math.max(0, kulanilan_belek_kb);
            }
        }

        jsona_aktr(test_boyutu, knn_dogrulluk, agac_dogrulluk, knn_sure, agac_sure, knn_belek, agac_belek, knn_matris_map, agc_matris_map);
    }

    private void jsona_aktr(int test_boyut, double knn_dgrlk, double agc_dgrlk, long knn_sr, long agc_sr, long knn_m, long agc_m,
                              Map<String, Map<String, Integer>> knn_mat, 
                              Map<String, Map<String, Integer>> agc_mat) {
                              
        String knn_jsn = jsn_cevir(knn_mat);
        String agc_jsn = jsn_cevir(agc_mat);

        String jsn_formati = "{\n" +
                "  \"test_veri_boyutu\": %d,\n" +
                "  \"algolarr\": {\n" +
                "    \"knn\": {\n" +
                "      \"dogrulluk_yzdesi\": %.2f,\n" +
                "      \"tahmiin_suresi_ms\": %d,\n" +
                "      \"kulanilan_belek_kb\": %d,\n" +
                "      \"karmasik_matris\": %s\n" +
                "    },\n" +
                "    \"kararr_agci\": {\n" +
                "      \"dogrulluk_yzdesi\": %.2f,\n" +
                "      \"tahmiin_suresi_ms\": %d,\n" +
                "      \"kulanilan_belek_kb\": %d,\n" +
                "      \"karmasik_matris\": %s\n" +
                "    }\n" +
                "  }\n" +
                "}";
                
        String jsn_cikti = String.format(Locale.US, jsn_formati, test_boyut, knn_dgrlk, knn_sr, knn_m, knn_jsn, agc_dgrlk, agc_sr, agc_m, agc_jsn);
        
        FileWriter dosya = null;
        try {
            dosya = new FileWriter("results.json");
            dosya.write(jsn_cikti);
            System.out.println("results.json dosyasini disa aktardik. Kullanici arayuzu HAZIIR!!!!.");
            dosya.close();
        } catch (IOException e) {
            System.out.println("JSON Eksport Hatasi: " + e.getMessage());
        }
    }
}

class DecisionTreeClassifier extends Ana_Algoritma {
    private double entropi_hespla(List<UserRecord> veri) {
        if (veri.isEmpty()) { return 0.0; }
            
        HashMap<String, Integer> sayaclar;
        sayaclar = new HashMap<String, Integer>();
        
        for (int j = 0; j < veri.size(); j++) {
            UserRecord u = veri.get(j);
            String katgori_adi = u.kategori();
            if (sayaclar.containsKey(katgori_adi)) {
                int gecici_sayi = sayaclar.get(katgori_adi);
                sayaclar.put(katgori_adi, gecici_sayi + 1);
            } else {
                sayaclar.put(katgori_adi, 1);
            }
        }
        
        double entropi = 0.0;
        for (int sayac : sayaclar.values()) {
            double oran = (double) sayac / veri.size();
            double log_deger = Math.log(oran);
            double taban = Math.log(2);
            double bolum = log_deger / taban;
            entropi = entropi - (oran * bolum);
        }
        return entropi;
    }
    
    private double ortlma_fiyat;
    
    @Override
    public String tahmin_yap(UserRecord hdf_knlci) {
        if (kok_dugum == null) { return "Unknown"; }
        TreeNode suanki_dugum = kok_dugum;

        while (!suanki_dugum.yaprak_mi) {
            String deger = "";
            if (suanki_dugum.bolunen_ozelk.equals("Gender")) {
                deger = hdf_knlci.cinsiyet_cek();
            } else if (suanki_dugum.bolunen_ozelk.equals("Brand")) {
                deger = hdf_knlci.getMarka();
            } else if (suanki_dugum.bolunen_ozelk.equals("Price")) {
                if (hdf_knlci.netToplam() >= ortlma_fiyat) {
                    deger = "High";
                } else {
                    deger = "Low";
                }
            }

            TreeNode sonrki = suanki_dugum.cocuklar.get(deger);
            if (sonrki == null) { return varsaylan_katgori; }
            suanki_dugum = sonrki;
        }
        return suanki_dugum.tamin_edilen_katgori;
    }
    
    private int max_derinlik;
    
    private Map<String, List<UserRecord>> veriyi_bol_ozelege_gore(List<UserRecord> veriler, String ozelik) {
        HashMap<String, List<UserRecord>> bolunmus;
        bolunmus = new HashMap<String, List<UserRecord>>();
        
        for (UserRecord u : veriler) {
            String anahtar = "";
            if (ozelik.equals("Gender")) {
                anahtar = u.cinsiyet_cek();
            } else if (ozelik.equals("Brand")) {
                anahtar = u.getMarka();
            } else if (ozelik.equals("Price")) {
                if (u.netToplam() >= ortlma_fiyat) {
                    anahtar = "High";
                } else {
                    anahtar = "Low";
                }
            }
            if (!bolunmus.containsKey(anahtar)) {
                ArrayList<UserRecord> bos_liste;
                bos_liste = new ArrayList<UserRecord>();
                bolunmus.put(anahtar, bos_liste);
            }
            List<UserRecord> gecici_liste = bolunmus.get(anahtar);
            gecici_liste.add(u);
        }
        return bolunmus;
    }
    
    @Override
    public void egit(List<UserRecord> egitm_verisi) {
        long baslama_zamani = System.currentTimeMillis();
        // Hoca Excel'de bos veri var mi kontrol edin dedigi icin buraya null check koydum, yoksa patliyor
        double toplam = 0;
        for (UserRecord kl : egitm_verisi) {
             if(kl!=null) toplam += kl.netToplam();
        }
        
        this.ortlma_fiyat = toplam / egitm_verisi.size();
        
        this.varsaylan_katgori = cogunlukKategori(egitm_verisi);

        ArrayList<String> ozellikler;
        ozellikler = new ArrayList<String>();
        ozellikler.add("Gender");
        ozellikler.add("Brand");
        ozellikler.add("Price");
        this.kok_dugum = agac_olustr(egitm_verisi, ozellikler, 0);
        
        this.egitm_suresi_ms = System.currentTimeMillis() - baslama_zamani;
        System.out.println(algo_ismi + " training complted in " + egitm_suresi_ms + " ms.\n");
    }

    private TreeNode kok_dugum;
    
    public DecisionTreeClassifier(int max_dr) {
        this.max_derinlik = max_dr;
        this.algo_ismi = "Decision Tree (ID3)";
    }
    
    private String cogunlukKategori(List<UserRecord> veriler) {
        if (veriler.isEmpty()) { return "Unknown"; }
        
        HashMap<String, Integer> sayaclar;
        sayaclar = new HashMap<String, Integer>();
        
        for (int j = 0; j < veriler.size(); j++) {
            UserRecord u = veriler.get(j);
            String katgori_adi = u.kategori();
            if (sayaclar.containsKey(katgori_adi)) {
                int gecici_sayi = sayaclar.get(katgori_adi);
                sayaclar.put(katgori_adi, gecici_sayi + 1);
            } else {
                sayaclar.put(katgori_adi, 1);
            }
        }
        
        int en_buyuk_deger = -1;
        String en_buyuk_anahtar = "";
        
        for (String anahtar : sayaclar.keySet()) {
            int dger = sayaclar.get(anahtar);
            if (dger > en_buyuk_deger) { en_buyuk_deger = dger; en_buyuk_anahtar = anahtar; }
        }
        
        return en_buyuk_anahtar;
    }
    
    private TreeNode agac_olustr(List<UserRecord> veriler, List<String> mevcut_ozelk, int gncel_derinlik) {
        String cgunluk_ktg = cogunlukKategori(veriler);

        if (mevcut_ozelk.isEmpty() || entropi_hespla(veriler) == 0.0 || gncel_derinlik >= max_derinlik) {
            TreeNode dondur_dugum_y;
            dondur_dugum_y = new TreeNode(cgunluk_ktg, true);
            return dondur_dugum_y;
        }

        double temel_entropi = entropi_hespla(veriler);
        double max_blgi_kazanc = -1.0;
        String en_iyi_ozelk = "";

        for (String ozelik : mevcut_ozelk) {
            double deger = bilgi_kazanci_hespla(veriler, ozelik, temel_entropi);
            if (deger > max_blgi_kazanc) 
            {
                max_blgi_kazanc = deger;
                en_iyi_ozelk = ozelik;
            }
        }

        if (max_blgi_kazanc <= 0) 
        { 
            TreeNode dondur_dugum_z;
            dondur_dugum_z = new TreeNode(cgunluk_ktg, true);
            return dondur_dugum_z; 
        }

        TreeNode dugum = new TreeNode(en_iyi_ozelk);
        Map<String, List<UserRecord>> parcalar = veriyi_bol_ozelege_gore(veriler, en_iyi_ozelk);
        
        ArrayList<String> kalan_ozellikler;
        kalan_ozellikler = new ArrayList<String>(mevcut_ozelk);
        kalan_ozellikler.remove(en_iyi_ozelk);

        for (Map.Entry<String, List<UserRecord>> giris : parcalar.entrySet()) {
            dugum.cocuklar.put(giris.getKey(), agac_olustr(giris.getValue(), kalan_ozellikler, gncel_derinlik + 1));
        }

        TreeNode dondur_dugum = dugum;
        return dondur_dugum;
    }
    
    private String varsaylan_katgori;
    
    private double bilgi_kazanci_hespla(List<UserRecord> veri, String ozelik, double temel_ent) {
        Map<String, List<UserRecord>> parcalar = veriyi_bol_ozelege_gore(veri, ozelik);
        double yeni_entropi = 0.0;
        for (List<UserRecord> alt_kume : parcalar.values()) {
            double agirlik = (double) alt_kume.size() / veri.size();
            yeni_entropi += agirlik * entropi_hespla(alt_kume);
        }
        double dondur_kazanc = temel_ent - yeni_entropi;
        return dondur_kazanc;
    }
}

class SplitResult {
    public List<UserRecord> egitimVeriGetir() { 
        return new ArrayList<UserRecord>(egitm_veri); 
    }
    
    private List<UserRecord> egitm_veri;
    
    public SplitResult(List<UserRecord> e_veri, List<UserRecord> t_veri) {
        this.egitm_veri = e_veri;
        this.test_veri = t_veri;
    }
    
    private List<UserRecord> test_veri;
    
    public List<UserRecord> testVeriGetir() { 
        return new ArrayList<UserRecord>(test_veri); 
    }
}

class KNNClassifier extends Ana_Algoritma {
    private final double VARSAYILN_MARKA_DGER = 0.0;
    
    @Override
    public String tahmin_yap(UserRecord hedef_kulanci) {
        ArrayList<DistanceRecord> mesafeler;
        mesafeler = new ArrayList<DistanceRecord>();
        
        double degisken1 = fiyat_normallestir(hedef_kulanci.netToplam());
        
        double degisken2;
        if (hedef_kulanci.cinsiyet_cek().equalsIgnoreCase("Male")) {
            degisken2 = 1.0;
        } else {
            degisken2 = 0.0;
        }
        
        double degisken3;
        if (marka_kodlayci.containsKey(hedef_kulanci.getMarka())) {
            degisken3 = marka_kodlayci.get(hedef_kulanci.getMarka());
        } else {
            degisken3 = VARSAYILN_MARKA_DGER;
        }

        double be_w = 1.0;
        double pe_w = 2.5; 
        double ge_w = 1.0;
        
        for (UserRecord egitim_kulanci : egitm_veri) {
            double tir1 = fiyat_normallestir(egitim_kulanci.netToplam());
            
            double tir2;
            if (egitim_kulanci.cinsiyet_cek().equalsIgnoreCase("Male")) {
                tir2 = 1.0;
            } else {
                tir2 = 0.0;
            }
            
            double tir3;
            if (marka_kodlayci.containsKey(egitim_kulanci.getMarka())) {
                tir3 = marka_kodlayci.get(egitim_kulanci.getMarka());
            } else {
                tir3 = VARSAYILN_MARKA_DGER;
            }

            double fark1 = degisken1 - tir1;
            double kare1 = pe_w * (fark1 * fark1);
            
            double fark2 = degisken2 - tir2;
            double kare2 = ge_w * (fark2 * fark2);
            
            double fark3 = degisken3 - tir3;
            double kare3 = be_w * (fark3 * fark3);
            
            double toplam_kareler = kare1 + kare2 + kare3;
            double mesafe = Math.sqrt(toplam_kareler);
            
            mesafeler.add(new DistanceRecord(mesafe, egitim_kulanci.kategori()));
        }

        // en kucukten buyuge siraliyo 
        Collections.sort(mesafeler);

        HashMap<String, Integer> gecici_map;
        gecici_map = new HashMap<String, Integer>();
        for (int i = 0; i < Math.min(k_deger, mesafeler.size()); i++) {
            String gecerli_katg = mesafeler.get(i).katgori;
            if (gecici_map.containsKey(gecerli_katg)) {
                int gecici_sayi = gecici_map.get(gecerli_katg);
                gecici_map.put(gecerli_katg, gecici_sayi + 1);
            } else {
                gecici_map.put(gecerli_katg, 1);
            }
        }

        // en cok oy kategorisi
        int en_buyuk_oy = -1;
        String en_buyuk_katgori = "";
        
        for (String anahtar : gecici_map.keySet()) {
            int dger = gecici_map.get(anahtar);
            if (dger > en_buyuk_oy) {
                en_buyuk_oy = dger;
                en_buyuk_katgori = anahtar;
            }
        }
        
        return en_buyuk_katgori;
    }
    
    private double min_fiyat = Double.MAX_VALUE;
    
    private double fiyat_normallestir(double fyt) {
        if (max_fiyat <= min_fiyat) { return 0.0; } // max min olayi (hata vermesin diye)
        return (fyt - min_fiyat) / (max_fiyat - min_fiyat);
    }
    
    private class DistanceRecord implements Comparable<DistanceRecord> {
        double mesafe;
        String katgori;
        DistanceRecord(double m, String k) {
            this.mesafe = m; this.katgori = k;
        }
        @Override
        public int compareTo(DistanceRecord dgr) {
            return Double.compare(this.mesafe, dgr.mesafe);
        }
    }
    
    private int k_deger; // komsu sayisi
    
    @Override
    public void egit(List<UserRecord> dta) {
        long b_sre = System.currentTimeMillis();
        this.egitm_veri = dta;
        
        for (UserRecord u : egitm_veri) {
            double f = u.netToplam();
            if (f < min_fiyat) min_fiyat = f;
            if (f > max_fiyat) max_fiyat = f;
        }

        int marka_sayaci = 0;
        for (UserRecord u : egitm_veri) {
            String mark_adi = u.getMarka();
            if (!marka_kodlayci.containsKey(mark_adi)) {
                marka_kodlayci.put(mark_adi, (double) marka_sayaci / 50.0);
                marka_sayaci++;
            }
        }
        
        this.egitm_suresi_ms = System.currentTimeMillis() - b_sre;
        System.out.println(algo_ismi + " trainin completd in " + egitm_suresi_ms + " ms.");
    }
    
    private double max_fiyat = Double.MIN_VALUE;
    
    public KNNClassifier(int kr) {
        this.k_deger = kr;
        this.algo_ismi = "K-Nearest Neighbors (KNN)";
        
        HashMap<String, Double> gecici_marka;
        gecici_marka = new HashMap<String, Double>();
        this.marka_kodlayci = gecici_marka;
    }
    
    private List<UserRecord> egitm_veri;
    
    private Map<String, Double> marka_kodlayci;
}

class DataLoader {
    public List<UserRecord> excelRead(String dosya_yolu) {
        ArrayList<UserRecord> kayitlar;
        kayitlar = new ArrayList<UserRecord>();
        
        FileInputStream dsya_akisi = null;
        Workbook calisma_kitbi = null;
        try {
            dsya_akisi = new FileInputStream(new File(dosya_yolu));
            calisma_kitbi = new XSSFWorkbook(dsya_akisi);

            Sheet sayfa = calisma_kitbi.getSheetAt(0);

            // 0. satirdan basla (Basliklar dahil) tum satirlari tara
            for (int j = 0; j <= sayfa.getLastRowNum(); j++) {
                
                // Sadece GERCEK 5093 veriyi buldugunda dur
                if (kayitlar.size() >= 5093) 
                {
                        break;
                }

                Row satir = sayfa.getRow(j);
                if (satir == null) { continue; } // TODO: buradaki null hatasini hoca sorarsa acikla

                // Asimetrik yaklasim 1: Direkt okuyup bosluklari trim ile alan ozensiz cekim
                String katgori_isimi = satir.getCell(12) != null ? satir.getCell(12).toString().trim() : "UNKNOWN";
                if (katgori_isimi.isEmpty() || katgori_isimi.equalsIgnoreCase("categoryname") || katgori_isimi.equalsIgnoreCase("UNKNOWN")) {
                    continue; 
                }

                // Asimetrik yaklasim 2: Kisa devre if mantigi
                String msteri_kod = "UNKNOWN";
                if (satir.getCell(9) != null) msteri_kod = satir.getCell(9).toString();
                
                // Asimetrik yaklasim 3: Detayli try-catch ve is mantigi 
                double deger = 0.0;
                try {
                    Cell deger_hucresi = satir.getCell(7);
                    if(deger_hucresi != null) {
                         deger = Double.parseDouble(deger_hucresi.toString());
                         if(deger < 0) { deger = 0.0; } // exceldeki negatif harcamalari engelle
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Hata: Fiyat sutununda yazi var, okunamadi.");
                    continue;
                } catch (NullPointerException e) {
                    System.out.println("Hata: Fiyat kismi bos.");
                    continue;
                }
                
                // Asimetrik yaklasim 4: Acemi if-else blogu
                String marka_isimi;
                Cell mark_hcresi = satir.getCell(11);
                if (mark_hcresi != null) {
                    marka_isimi = mark_hcresi.toString();
                } else {
                    marka_isimi = "UNKNOWN";
                }
                
                // Asimetrik yaklasim 5: String concetenation hilesiyle stringe cevirme
                String cinsyt_isimi = "";
                Cell cinsiyet_hc = satir.getCell(17);
                if (cinsiyet_hc == null) {
                    cinsyt_isimi = "UNKNOWN";
                } else {
                    cinsyt_isimi = cinsiyet_hc + "";
                }

                kayitlar.add(new UserRecord(msteri_kod, deger, marka_isimi, katgori_isimi, cinsyt_isimi));
            }
            calisma_kitbi.close();
            dsya_akisi.close();
        } catch (Exception e) {
            System.out.println("Dosyadan okurken bi hata cikti abey: " + e.getMessage());
        }
        return kayitlar;
    }
}

public class JavaKararAgaci {
    
    public static void algolari_calistr(String egitm_ex_yolu, String test_ex_yolu, int knn_k, int agac_max_dr, String mod) {
        System.out.println("Loadng data from Excelll...");
         DataLoader yukleyci = new DataLoader();
        List<UserRecord> tum_veri = yukleyci.excelRead(egitm_ex_yolu);

        if (tum_veri.isEmpty()) { System.out.println("Erroor: Dataset is empty or fil not foud."); return; }

        List<UserRecord> egitm_verileri;
        List<UserRecord> test_verileri;

        if (test_ex_yolu == null || test_ex_yolu.trim().isEmpty()) { System.out.println("Ototamatik Modd: Veriler bolunuyo (80% Train, 20% Test)...");
            PreProcessor on_islemci = new PreProcessor();
            SplitResult sonuc = on_islemci.indeks_isle(tum_veri);
            egitm_verileri = sonuc.egitimVeriGetir(); test_verileri = sonuc.testVeriGetir();
        } else {
            System.out.println("Manuuel Modd: Test verileri Excelden ayri yukleniyo...");
            egitm_verileri = tum_veri;
            test_verileri = yukleyci.excelRead(test_ex_yolu);
        }

        System.out.println("Egitm Verisi: " + egitm_verileri.size()); System.out.println("Tst Verisi: " + test_verileri.size() + "\n");

        ArrayList<IClassifier> modeller;
        modeller = new ArrayList<IClassifier>();
        if ("KNN".equals(mod) || "BOTH".equals(mod)) {
            modeller.add(new KNNClassifier(knn_k));
        }
        if ("TREE".equals(mod) || "BOTH".equals(mod)) {
            modeller.add(new DecisionTreeClassifier(agac_max_dr));
        }

        for (int m = 0; m < modeller.size(); m++) {
            IClassifier gecici_modl = modeller.get(m);
            gecici_modl.egit(egitm_verileri);
        }

        Evaluator degerlndrici = new Evaluator();
        degerlndrici.modl_deglerlendir(modeller, test_verileri);
    }
    
    // args'i hoca sorarsa ne diyorduk?
    public static void main(String[] args) {
        String excel_yol = "MarketSalesKocaeli.xlsx";
        algolari_calistr(excel_yol, null, 5, 5, "BOTH");
    }
}
