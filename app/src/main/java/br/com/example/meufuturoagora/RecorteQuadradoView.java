package br.com.example.meufuturoagora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

// Área de recorte da foto de perfil: a imagem pode ser arrastada e ampliada com
// dois dedos por baixo de uma moldura quadrada fixa. A moldura sempre fica
// coberta pela imagem, então o recorte nunca tem bordas vazias.
public class RecorteQuadradoView extends View {

    private static final float ZOOM_MAXIMO = 6f;

    private Bitmap imagem;
    private final Matrix matriz = new Matrix();
    private final RectF moldura = new RectF();

    private final Paint pintaImagem = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaSombra = new Paint();
    private final Paint pintaBorda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaGrade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path caminhoSombra = new Path();

    private float escalaMinima = 1f;
    private float ultimoX;
    private float ultimoY;
    private int dedoAtivo = MotionEvent.INVALID_POINTER_ID;

    private final ScaleGestureDetector detectorZoom;

    public RecorteQuadradoView(Context context) {
        this(context, null);
    }

    public RecorteQuadradoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float densidade = getResources().getDisplayMetrics().density;

        pintaSombra.setColor(Color.argb(160, 0, 0, 0));

        pintaBorda.setColor(Color.WHITE);
        pintaBorda.setStyle(Paint.Style.STROKE);
        pintaBorda.setStrokeWidth(2.5f * densidade);

        pintaGrade.setColor(Color.argb(110, 255, 255, 255));
        pintaGrade.setStyle(Paint.Style.STROKE);
        pintaGrade.setStrokeWidth(1f * densidade);

        caminhoSombra.setFillType(Path.FillType.EVEN_ODD);

        detectorZoom = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                float atual = escalaAtual();
                float fator = detector.getScaleFactor();
                float nova = Math.max(escalaMinima, Math.min(atual * fator, escalaMinima * ZOOM_MAXIMO));

                fator = nova / atual;
                matriz.postScale(fator, fator, detector.getFocusX(), detector.getFocusY());
                limitar();
                invalidate();
                return true;
            }
        });
    }

    void setImagem(Bitmap bitmap) {
        imagem = bitmap;
        ajustarInicio();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int largura, int altura, int larguraAntiga, int alturaAntiga) {
        super.onSizeChanged(largura, altura, larguraAntiga, alturaAntiga);
        ajustarInicio();
    }

    // Moldura centralizada; imagem centralizada cobrindo a moldura
    private void ajustarInicio() {

        if (imagem == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float margem = 24 * getResources().getDisplayMetrics().density;
        float lado = Math.min(getWidth(), getHeight()) - 2 * margem;
        float esquerda = (getWidth() - lado) / 2f;
        float topo = (getHeight() - lado) / 2f;

        moldura.set(esquerda, topo, esquerda + lado, topo + lado);

        caminhoSombra.reset();
        caminhoSombra.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        caminhoSombra.addRect(moldura, Path.Direction.CW);

        escalaMinima = Math.max(lado / imagem.getWidth(), lado / imagem.getHeight());

        matriz.reset();
        matriz.postScale(escalaMinima, escalaMinima);
        matriz.postTranslate(
                moldura.centerX() - imagem.getWidth() * escalaMinima / 2f,
                moldura.centerY() - imagem.getHeight() * escalaMinima / 2f
        );
    }

    private float escalaAtual() {
        float[] valores = new float[9];
        matriz.getValues(valores);
        return valores[Matrix.MSCALE_X];
    }

    // Impede que a imagem deixe parte da moldura descoberta
    private void limitar() {

        RectF limites = new RectF(0, 0, imagem.getWidth(), imagem.getHeight());
        matriz.mapRect(limites);

        float dx = 0;
        float dy = 0;

        if (limites.left > moldura.left) dx = moldura.left - limites.left;
        if (limites.right < moldura.right) dx = moldura.right - limites.right;
        if (limites.top > moldura.top) dy = moldura.top - limites.top;
        if (limites.bottom < moldura.bottom) dy = moldura.bottom - limites.bottom;

        matriz.postTranslate(dx, dy);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {

        if (imagem == null) {
            return false;
        }

        detectorZoom.onTouchEvent(evento);

        switch (evento.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                dedoAtivo = evento.getPointerId(0);
                ultimoX = evento.getX();
                ultimoY = evento.getY();
                break;

            case MotionEvent.ACTION_MOVE: {

                int indice = evento.findPointerIndex(dedoAtivo);

                if (indice < 0) {
                    break;
                }

                float x = evento.getX(indice);
                float y = evento.getY(indice);

                if (!detectorZoom.isInProgress()) {
                    matriz.postTranslate(x - ultimoX, y - ultimoY);
                    limitar();
                    invalidate();
                }

                ultimoX = x;
                ultimoY = y;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                // Se o dedo que arrastava saiu, continua com o outro
                int indice = evento.getActionIndex();

                if (evento.getPointerId(indice) == dedoAtivo) {

                    int novo = indice == 0 ? 1 : 0;
                    dedoAtivo = evento.getPointerId(novo);
                    ultimoX = evento.getX(novo);
                    ultimoY = evento.getY(novo);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dedoAtivo = MotionEvent.INVALID_POINTER_ID;
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imagem == null) {
            return;
        }

        canvas.drawBitmap(imagem, matriz, pintaImagem);
        canvas.drawPath(caminhoSombra, pintaSombra);

        // Grade 3x3 para ajudar a enquadrar
        float terco = moldura.width() / 3f;

        for (int i = 1; i <= 2; i++) {
            canvas.drawLine(moldura.left + terco * i, moldura.top, moldura.left + terco * i, moldura.bottom, pintaGrade);
            canvas.drawLine(moldura.left, moldura.top + terco * i, moldura.right, moldura.top + terco * i, pintaGrade);
        }

        canvas.drawRect(moldura, pintaBorda);
    }

    // Parte da imagem dentro da moldura, como um bitmap quadrado de "tamanho" pixels
    Bitmap recortar(int tamanho) {

        if (imagem == null) {
            return null;
        }

        Matrix inversa = new Matrix();
        matriz.invert(inversa);

        RectF origem = new RectF(moldura);
        inversa.mapRect(origem);

        Rect origemInteira = new Rect(
                Math.max(0, Math.round(origem.left)),
                Math.max(0, Math.round(origem.top)),
                Math.min(imagem.getWidth(), Math.round(origem.right)),
                Math.min(imagem.getHeight(), Math.round(origem.bottom))
        );

        int lado = Math.min(tamanho, Math.max(1, origemInteira.width()));

        Bitmap resultado = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888);
        Canvas tela = new Canvas(resultado);
        tela.drawColor(Color.WHITE);
        tela.drawBitmap(imagem, origemInteira, new Rect(0, 0, lado, lado), pintaImagem);

        return resultado;
    }
}
