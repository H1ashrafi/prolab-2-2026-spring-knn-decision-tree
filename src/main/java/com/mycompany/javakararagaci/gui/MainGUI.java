package com.mycompany.javakararagaci.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.javakararagaci.JavaKararAgaci;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class MainGUI extends JFrame {

    private void mod_gncele() {
        if (txt_test_yolu.getText().trim().isEmpty()) {
            lbl_mod_durmu.setText("  | Aktif Mod: OTTOMATIK (%80 Egitiim, %20 Test)");
            lbl_mod_durmu.setForeground(new Color(0, 128, 0));
        } else {
            lbl_mod_durmu.setText("  | Aktif Mod: MANNUEL (Egitiim ve Test Ayri)");
            lbl_mod_durmu.setForeground(Color.RED);
        }
    }
    
    // bu degiskenler asagida lazim oluyor
    private JTextField txt_excel_yolu;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MainGUI().setVisible(true);
        });
    }

    private JTextField txt_knn_k;
    private JTextField txt_agac_derinlik;
    
    // pencereyi burda actim
    public MainGUI() {
        setTitle("Kararr Agaci ve KNN Analliz Arayyuzu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        arayzu_hazirla();
    }
    
    private void calistr_ve_sonclari_yukle() {
        String excel_yolu = txt_excel_yolu.getText().trim();
        String test_yolu = txt_test_yolu.getText().trim();
        int knn_deger = 5;
        int derinlik = 5;

        try {
            knn_deger = Integer.parseInt(txt_knn_k.getText().trim());
            derinlik = Integer.parseInt(txt_agac_derinlik.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Luttfen K ve Max Depth icin gecerli tam sayiilar girinn.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String mod_secimi = "BOTH";
        int secilen_id = cmb_degerlndirme_modi.getSelectedIndex();
        
          if (secilen_id == 0) {
               mod_secimi = "KNN";
          }
          else if (secilen_id == 1) {
               mod_secimi = "TREE";
          }
        
        final String final_mod = mod_secimi;

        txt_knn_sonuclar.setText("Caliisiyoor... Bekleyyiniz.");
        txt_agac_sonuclar.setText("Caliisiyoor... Bekleyyiniz.");

        try {
            // Arka plan işlemini thread içinde çalıştıralım ki UI donmasın
            int sonuc_k = knn_deger;
            int sonuc_derinlik = derinlik;
            new Thread(() -> {
                try {
                    // Backend'i tetikle
                    JavaKararAgaci.algolari_calistr(excel_yolu, test_yolu, sonuc_k, sonuc_derinlik, final_mod);
                    
                    // İşlem bitince UI'ı güncelle
                    SwingUtilities.invokeLater(() -> jsondan_sonclari_yukle(final_mod));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MainGUI.this, "Calistirma Hattasi: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTextArea txt_knn_sonuclar;
    private JPanel pnl_grafik_tmp; // eski adini unuttum
    
    private void jsondan_sonclari_yukle(String modu) {
        File json_dosyasi = new File("results.json");
        if (!json_dosyasi.exists()) {
            JOptionPane.showMessageDialog(this, "resullts.json bullunammadi! Bakkend islemmi basarisiz olmus olabiilir.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (FileReader okuycu = new FileReader(json_dosyasi)) {
            Gson gson_nesnesi = new Gson();
            JsonObject kok_obje = gson_nesnesi.fromJson(okuycu, JsonObject.class);
            JsonObject algolar_obje = kok_obje.getAsJsonObject("algolarr");

            txt_knn_sonuclar.setText("");
            txt_agac_sonuclar.setText("");

            double knn_dogrulluk = 0; long knn_suresi = 0; long knn_beleki = 0;
            double agac_dogrulluk = 0; long agac_suresi = 0; long agac_beleki = 0;

            if ("KNN".equals(modu) || "BOTH".equals(modu)) {
                JsonObject knn_objesi = algolar_obje.getAsJsonObject("knn");
                if (knn_objesi != null && knn_objesi.has("dogrulluk_yzdesi")) {
                    knn_dogrulluk = knn_objesi.get("dogrulluk_yzdesi").getAsDouble();
                    knn_suresi = knn_objesi.get("tahmiin_suresi_ms").getAsLong();
                    
                    if (knn_objesi.has("kulanilan_belek_kb")) {
                        knn_beleki = knn_objesi.get("kulanilan_belek_kb").getAsLong();
                    } else {
                        knn_beleki = 0;
                    }
                    
                    JsonObject karmasik_matris = knn_objesi.getAsJsonObject("karmasik_matris");
                    snc_metni_gncele(txt_knn_sonuclar, "K-Nearest Neighbors", knn_dogrulluk, knn_suresi, knn_beleki, karmasik_matris);
                }
            } else {
                txt_knn_sonuclar.setText("Sadecce Karar Agacci seciildigi iciin KNN calistrillmadi.");
            }

            if ("TREE".equals(modu) || "BOTH".equals(modu)) {
                JsonObject agac_objesi = algolar_obje.getAsJsonObject("kararr_agci");
                if (agac_objesi != null && agac_objesi.has("dogrulluk_yzdesi")) {
                    agac_dogrulluk = agac_objesi.get("dogrulluk_yzdesi").getAsDouble();
                    agac_suresi = agac_objesi.get("tahmiin_suresi_ms").getAsLong();
                    
                    if (agac_objesi.has("kulanilan_belek_kb")) {
                        agac_beleki = agac_objesi.get("kulanilan_belek_kb").getAsLong();
                    } else {
                        agac_beleki = 0;
                    }
                    
                    JsonObject karmasik_matris2 = agac_objesi.getAsJsonObject("karmasik_matris");
                    snc_metni_gncele(txt_agac_sonuclar, "Decision Tree", agac_dogrulluk, agac_suresi, agac_beleki, karmasik_matris2);
                }
            } else {
                txt_agac_sonuclar.setText("Saddece KNN seccildigi iciin Karar Aagaci callistirilmmadi.");
            }

            grafkleri_gncele(knn_dogrulluk, agac_dogrulluk, knn_suresi, agac_suresi, knn_beleki, agac_beleki, modu);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "JSON oukkuma hatasi: " + ex.getMessage(), "Haata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JComboBox<String> cmb_degerlndirme_modi;

    // TODO: grafik burda ciziliyor ellemeyin patlar
    private void grafkleri_gncele(double kn_dogru, double ag_dogru, long kn_sure, long ag_sure, long kn_belek, long ag_belek, String modu) {
        pnl_grafik_tmp.removeAll();

        // Accuracy Chart
        DefaultCategoryDataset data_seti1;
        data_seti1 = new DefaultCategoryDataset();
        
          if ("KNN".equals(modu) || "BOTH".equals(modu)) {
              data_seti1.addValue(kn_dogru, "Accuracy", "KNN");
          }
        if ("TREE".equals(modu) || "BOTH".equals(modu)) {
            data_seti1.addValue(ag_dogru, "Accuracy", "Decision Tree");
        }

        JFreeChart grafik_obje1 = ChartFactory.createBarChart(
                "Dogrulluk (Accuaraacy) Karisilastrmasi",
                "Alogritma",
                "Dogrulluk (%)",
                data_seti1,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Time Chart
      DefaultCategoryDataset data_seti2;
      data_seti2 = new DefaultCategoryDataset();
      
        if ("KNN".equals(modu) || "BOTH".equals(modu)) {
            data_seti2.addValue(kn_sure, "Time (ms)", "KNN");
        }
          if ("TREE".equals(modu) || "BOTH".equals(modu)) {
              data_seti2.addValue(ag_sure, "Time (ms)", "Decision Tree");
          }

        JFreeChart grafik_obje2 = ChartFactory.createBarChart(
                "Tahmin Suressi Karsilaastirmasi",
                "Allgoritma",
                "Suresi (ms)",
                data_seti2,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Memory Chart
        DefaultCategoryDataset data_seti3;
        data_seti3 = new DefaultCategoryDataset();
        
        if ("KNN".equals(modu) || "BOTH".equals(modu)) {
            data_seti3.addValue(kn_belek, "Memory (KB)", "KNN");
        }
        if ("TREE".equals(modu) || "BOTH".equals(modu)) {
            data_seti3.addValue(ag_belek, "Memory (KB)", "Decision Tree");
        }

        JFreeChart grafik_obje3 = ChartFactory.createBarChart(
                "Belek Kullaanimi Kaarsilastirrmasi",
                "Allgoritma",
                "Belek (KB)",
                data_seti3,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel panel_bir = new ChartPanel(grafik_obje1);
        ChartPanel panel_iki = new ChartPanel(grafik_obje2);
        ChartPanel panel_uc = new ChartPanel(grafik_obje3);

         pnl_grafik_tmp.add(panel_bir);
        pnl_grafik_tmp.add(panel_iki);
          pnl_grafik_tmp.add(panel_uc);

        pnl_grafik_tmp.revalidate();
        pnl_grafik_tmp.repaint();
    }
    
    private void snc_metni_gncele(JTextArea txt_alani, String model_adi, double dogruluk, long suresi, long beleki, JsonObject matris) {
        StringBuilder yazi_olusturcu = new StringBuilder();
        yazi_olusturcu.append("Moddel: ").append(model_adi).append("\n");
        yazi_olusturcu.append("Accuraccy (Dogrulluk): %").append(String.format(Locale.US, "%.2f", dogruluk)).append("\n");
        yazi_olusturcu.append("Tahmiin Sureesii: ").append(suresi).append(" ms\n");
        yazi_olusturcu.append("Bellekk Kulanmii: ").append(beleki).append(" KB\n\n");
        yazi_olusturcu.append("--- CONFUSION MATRIXX ---\n");

        for (String etiket : matris.keySet()) {
            yazi_olusturcu.append("Gercekler [").append(etiket).append("] -> Taminler: ");
            JsonObject gecici_obje = matris.getAsJsonObject(etiket);
            for (String ism : gecici_obje.keySet()) {
                yazi_olusturcu.append(ism).append("=").append(gecici_obje.get(ism).getAsInt()).append(", ");
            }
            yazi_olusturcu.setLength(yazi_olusturcu.length() - 2); // Son virgülü sil
            yazi_olusturcu.append("\n");
        }
        txt_alani.setText(yazi_olusturcu.toString());
    }
    
    private JTextArea txt_agac_sonuclar;
    private JTextField txt_test_yolu;
    
    private void dosya_sec(JTextField hedef_alan) {
        JFileChooser dosya_secici = new JFileChooser(new File("."));
        dosya_secici.setDialogTitle("Exceel Verri Seetti Secciniz");
        int sonuc_kodu = dosya_secici.showOpenDialog(this);
        if (sonuc_kodu == JFileChooser.APPROVE_OPTION) {
            hedef_alan.setText(dosya_secici.getSelectedFile().getAbsolutePath());
        }
    }
    
    private JLabel lbl_mod_durmu;
    private JLabel lbl_knn_k;
    private JLabel lbl_agac_derinlik;
    
    private void arayzu_hazirla() {
        // TOP PANEL - INPUTS
        JPanel ust_panel = new JPanel(new GridLayout(4, 1, 5, 5));
        ust_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel dosya_paneli = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dosya_paneli.add(new JLabel("Ana Veri / Egiitim Dosyassi (Exceel):"));
        txt_excel_yolu = new JTextField("MarketSalesKocaeli.xlsx", 30);
        dosya_paneli.add(txt_excel_yolu);
        JButton btn_dosya_sec = new JButton("Dosyya Secc");
        btn_dosya_sec.addActionListener(e -> dosya_sec(txt_excel_yolu));
        dosya_paneli.add(btn_dosya_sec);
        ust_panel.add(dosya_paneli);

        JPanel test_dosya_paneli = new JPanel(new FlowLayout(FlowLayout.LEFT));
        test_dosya_paneli.add(new JLabel("Tesst Dosyassi (Istegee Baglli):"));
        txt_test_yolu = new JTextField("", 30);
        test_dosya_paneli.add(txt_test_yolu);
        JButton btn_test_sec = new JButton("Dossya Secc");
        btn_test_sec.addActionListener(e -> dosya_sec(txt_test_yolu));
        test_dosya_paneli.add(btn_test_sec);
        
        lbl_mod_durmu = new JLabel("  | Aktif Mood: OTOMMATIKK (%80 Egiitim, %20 Teesst)");
        lbl_mod_durmu.setForeground(new Color(0, 128, 0));
        test_dosya_paneli.add(lbl_mod_durmu);
        
        txt_test_yolu.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { mod_gncele(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { mod_gncele(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { mod_gncele(); }
        });
        ust_panel.add(test_dosya_paneli);

        JPanel parametre_paneli = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lbl_knn_k = new JLabel("KNNN 'K' Dgeerii:");
        parametre_paneli.add(lbl_knn_k);
        txt_knn_k = new JTextField("5", 5);
        parametre_paneli.add(txt_knn_k);
        parametre_paneli.add(Box.createHorizontalStrut(20));
        
        lbl_agac_derinlik = new JLabel("Kaarar Agaaci 'Max Deeppth':");
        parametre_paneli.add(lbl_agac_derinlik);
        txt_agac_derinlik = new JTextField("5", 5);
        parametre_paneli.add(txt_agac_derinlik);
        
        parametre_paneli.add(Box.createHorizontalStrut(20));
        parametre_paneli.add(new JLabel("Moodel Seccimi:"));
        cmb_degerlndirme_modi = new JComboBox<>(new String[]{"Saddcece KNNN", "Saddecee Karrar Agaacci", "Ikiisini karsillastir"});
        cmb_degerlndirme_modi.setSelectedIndex(2);
        
        cmb_degerlndirme_modi.addActionListener(e -> {
            int secilmis_id = cmb_degerlndirme_modi.getSelectedIndex();
            
            boolean knn_goster = false;
            if (secilmis_id == 0 || secilmis_id == 2) {
                knn_goster = true;
            }
            
            boolean agac_goster = false;
            if (secilmis_id == 1 || secilmis_id == 2) {
                agac_goster = true;
            }
            
            lbl_knn_k.setVisible(knn_goster);
            txt_knn_k.setVisible(knn_goster);
            
            lbl_agac_derinlik.setVisible(agac_goster);
            txt_agac_derinlik.setVisible(agac_goster);
            
            parametre_paneli.revalidate();
            parametre_paneli.repaint();
        });
        
        parametre_paneli.add(cmb_degerlndirme_modi);
        
        ust_panel.add(parametre_paneli);

        JPanel aksiyon_paneli = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // bu buton ne yapiyordu hatirla
        JButton btn_calistir = new JButton("Modelari Callistir");
        btn_calistir.setFont(new Font("Arial", Font.BOLD, 14));
        btn_calistir.setBackground(new Color(60, 130, 200));
        btn_calistir.setForeground(Color.BLACK);
        btn_calistir.addActionListener(e -> calistr_ve_sonclari_yukle());
        aksiyon_paneli.add(btn_calistir);
        ust_panel.add(aksiyon_paneli);

        add(ust_panel, BorderLayout.NORTH);

        // CENTER PANEL - TEXT RESULTS
        JPanel merkez_panel = new JPanel(new GridLayout(1, 2, 10, 10));
        merkez_panel.setBorder(BorderFactory.createTitledBorder("Metiinsel Sonucllar"));

        txt_knn_sonuclar = new JTextArea();
        txt_knn_sonuclar.setEditable(false);
        txt_knn_sonuclar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane knn_kaydirma = new JScrollPane(txt_knn_sonuclar);
        knn_kaydirma.setBorder(BorderFactory.createTitledBorder("KNNN Sounuclaari"));

        txt_agac_sonuclar = new JTextArea();
        txt_agac_sonuclar.setEditable(false);
        txt_agac_sonuclar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane agac_kaydirma = new JScrollPane(txt_agac_sonuclar);
        agac_kaydirma.setBorder(BorderFactory.createTitledBorder("Karrar Agacii Sonuclari"));

        merkez_panel.add(knn_kaydirma);
        merkez_panel.add(agac_kaydirma);
        add(merkez_panel, BorderLayout.CENTER);

        // BOTTOM PANEL - CHARTS
        pnl_grafik_tmp = new JPanel(new GridLayout(1, 3, 10, 10));
        pnl_grafik_tmp.setPreferredSize(new Dimension(1000, 300));
        pnl_grafik_tmp.setBorder(BorderFactory.createTitledBorder("Graafiksell Kaarsilasitirma"));
        add(pnl_grafik_tmp, BorderLayout.SOUTH);
    }
}
