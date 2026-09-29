import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateIcons {
    public static void main(String[] args) {
        int[] sizes = {48, 72, 96, 144, 192};
        String[] folders = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
        String basePath = "app/src/main/res/";

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            String folder = folders[i];
            File dir = new File(basePath + folder);
            dir.mkdirs();

            // Delete existing webp
            new File(dir, "ic_launcher.webp").delete();
            new File(dir, "ic_launcher_round.webp").delete();

            // Square / Adaptive Legacy
            BufferedImage square = renderIcon(size, false);
            try {
                ImageIO.write(square, "PNG", new File(dir, "ic_launcher.png"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Round
            BufferedImage round = renderIcon(size, true);
            try {
                ImageIO.write(round, "PNG", new File(dir, "ic_launcher_round.png"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Java generated crisp PNG icons successfully for all densities!");
    }

    private static BufferedImage renderIcon(int size, boolean isRound) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background
        Color topColor = new Color(217, 119, 6);   // #D97706
        Color botColor = new Color(180, 83, 9);    // #B45309
        GradientPaint bgGrad = new GradientPaint(0, 0, topColor, size, size, botColor);
        g2.setPaint(bgGrad);

        if (isRound) {
            g2.fill(new Ellipse2D.Float(0, 0, size, size));
        } else {
            float corner = size * 0.22f;
            g2.fill(new RoundRectangle2D.Float(0, 0, size, size, corner, corner));
        }

        // Golden Sun Glow
        float center = size / 2.0f;
        float sunR = size * 0.24f;
        g2.setPaint(new GradientPaint(center - sunR, center - sunR, new Color(255, 251, 235), center + sunR, center + sunR, new Color(253, 230, 138)));
        g2.fill(new Ellipse2D.Float(center - sunR, center * 0.40f - sunR * 0.5f, sunR * 2, sunR * 2));

        // White/Cream Calendar Plate
        float calW = size * 0.56f;
        float calH = size * 0.44f;
        float calX = (size - calW) / 2.0f;
        float calY = size * 0.46f;
        float calRad = size * 0.08f;

        g2.setColor(new Color(250, 248, 245));
        g2.fill(new RoundRectangle2D.Float(calX, calY, calW, calH, calRad, calRad));

        // Calendar Amber Header
        g2.setColor(new Color(180, 83, 9));
        g2.fill(new RoundRectangle2D.Float(calX, calY, calW, calH * 0.32f, calRad, calRad));

        // Calendar Rings
        g2.setColor(new Color(120, 53, 15));
        float ringW = size * 0.04f;
        float ringH = size * 0.07f;
        g2.fill(new RoundRectangle2D.Float(calX + calW * 0.25f, calY - ringH * 0.4f, ringW, ringH, ringW, ringW));
        g2.fill(new RoundRectangle2D.Float(calX + calW * 0.70f, calY - ringH * 0.4f, ringW, ringH, ringW, ringW));

        // Calendar Matrix Dots
        g2.setColor(new Color(217, 119, 6));
        float dotR = size * 0.025f;
        float dotStartY = calY + calH * 0.54f;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                float dx = calX + calW * 0.22f + c * (calW * 0.28f);
                float dy = dotStartY + r * (calH * 0.22f);
                if (r == 1 && c == 1) {
                    g2.setColor(new Color(13, 148, 136)); // Turquoise accent
                } else {
                    g2.setColor(new Color(217, 119, 6));
                }
                g2.fill(new Ellipse2D.Float(dx - dotR, dy - dotR, dotR * 2, dotR * 2));
            }
        }

        g2.dispose();
        return img;
    }
}
