package com.onyx.dailydiary.writer;

import static java.lang.Integer.max;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.dailydiary.R;
import com.onyx.dailydiary.databinding.ActivityWriterBinding;
import com.onyx.dailydiary.utils.BitmapView;
import com.onyx.dailydiary.utils.GestureListener;
import com.onyx.dailydiary.utils.PenCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WriterActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = WriterActivity.class.getSimpleName();

    private ActivityWriterBinding binding;

    private GestureDetectorCompat mDetector;

    private TouchHelper touchHelper;
    private static final float STROKE_WIDTH = 4.0f;

    private final String filepath = "DailyDiary";
    private String filename;
    private final List<Rect> limitRectList = new ArrayList<>();


    private PenCallback penCallback;
    String currentdatestring;
    int daypage;
    int daypageCount;
    TextView datebox;
    LocalDate currentdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        binding = ActivityWriterBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        List<BitmapView> viewList = new ArrayList<>();
        viewList.add(binding.writerview);
        penCallback = new PenCallback(this, viewList);
        touchHelper = TouchHelper.create(getWindow().getDecorView().getRootView(), penCallback);
        touchHelper.debugLog(false);
        touchHelper.setRawInputReaderEnable(true);
        penCallback.setTouchHelper(touchHelper);

        // setup the gestures
        mDetector = new GestureDetectorCompat(this, new GestureListener() {
            @Override
            public void onSwipeBottom() {
                if (!penCallback.isRawDrawing()) {
                    deletePage();
                }
            }

            @Override
            public void onSwipeLeft() {
                if (!penCallback.isRawDrawing()) {
                    updatePage(true);
                }
            }

            @Override
            public void onSwipeRight() {
                if (!penCallback.isRawDrawing()) {
                    updatePage(false);
                }
            }

            @Override
            public void onSwipeTop() {
                if (!penCallback.isRawDrawing()) {
                    addPage();
                }
            }
        });


        // setup the date
        currentdatestring = getIntent().getStringExtra("date-string");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy");
        currentdate = LocalDate.parse(currentdatestring, formatter);

        daypage = 1;
        daypageCount = countDayPages();

        datebox = findViewById(R.id.date_text);
        datebox.setText(currentdate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + daypage + "/" + daypageCount + ")");
        filename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + daypage + ".png";
        // initialise surface
        initSurfaceView();

        // setup the buttons
        ImageButton back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        ImageButton nextpage_button = findViewById(R.id.nextpage);
        nextpage_button.setOnClickListener(this);

        ImageButton prevpage_button = findViewById(R.id.prevpage);
        prevpage_button.setOnClickListener(this);

        ImageButton addpage_button = findViewById(R.id.addpage);
        addpage_button.setOnClickListener(this);

        ImageButton deletepage_button = findViewById(R.id.deletepage);
        deletepage_button.setOnClickListener(this);

        ImageButton savepage_button = findViewById(R.id.save);
        savepage_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_button) {
            onBackPressed();
        } else if (id == R.id.nextpage) {
            updatePage(true);
        } else if (id == R.id.prevpage) {
            updatePage(false);
        } else if (id == R.id.addpage) {
            addPage();
        } else if (id == R.id.deletepage) {
            deletePage();
        } else if (id == R.id.save) {
            savePages();
        }
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        startTouchHelper();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }


    @Override
    protected void onPause() {
        super.onPause();
        binding.writerview.redrawSurface();
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.closeRawDrawing();
        if (penCallback.needsSave())
            binding.writerview.saveBitmap();

    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (penCallback.needsSave())
            binding.writerview.saveBitmap();

    }

    private void initSurfaceView() {

        binding.writerview.setBackground(R.drawable.page_bkgrnd);
        binding.writerview.setFilepath(filepath);
        binding.writerview.setFilename(filename);

        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
//                Rect limit = new Rect();
                Rect limit = new Rect();
                binding.writerview.getGlobalVisibleRect(limit);
                limitRectList.add(limit);
                startTouchHelper();
                binding.writerview.redrawSurface();
                Log.d(TAG, "surfaceCreated");
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        };
        binding.writerview.getHolder().addCallback(surfaceCallback);
    }

    private void startTouchHelper() {
        if (limitRectList.size() < 1) {
            return;
        }
        Log.d(TAG, "startTouchHelper");

        touchHelper.setStrokeWidth(STROKE_WIDTH);
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_MARKER);
        touchHelper.setStrokeColor(Color.BLACK);
        touchHelper.setLimitRect(limitRectList, new ArrayList<>())
                .openRawDrawing();

        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setMultiRegionMode();
        touchHelper.setRawDrawingEnabled(true);
        touchHelper.enableFingerTouch(true);
        touchHelper.setRawDrawingRenderEnabled(true);

    }


    private int countDayPages() {
        File dir = getExternalFilesDir(filepath);
        File[] files = dir.listFiles((dir1, name) ->
                name.contains(currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
        return max(files.length, 1);
    }

    private void updatePage(boolean forward) {
        touchHelper.setRawDrawingEnabled(false);
        touchHelper.setRawDrawingRenderEnabled(false);
        touchHelper.closeRawDrawing();

        // move forward or backwards in the diary
        if (penCallback.needsSave())
            binding.writerview.saveBitmap();

        if (forward) {
            if (daypage < daypageCount) {
                daypage++;
            } else {
                daypage = 1;
                currentdate = currentdate.plusDays(1);
                daypageCount = countDayPages();
            }

        } else {
            if (daypage > 1) {
                daypage--;
            } else {
                currentdate = currentdate.plusDays(-1);
                daypageCount = countDayPages();
                daypage = daypageCount;
            }
        }


        datebox = findViewById(R.id.date_text);
        datebox.setText(currentdate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + daypage + "/" + daypageCount + ")");

        filename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + daypage + ".png";
        binding.writerview.setFilename(filename);
        binding.writerview.redrawSurface();
        startTouchHelper();
    }

    private void addPage() {
        // add a page to the end and move forward

        penCallback.setNeedsSave(true);
        binding.writerview.saveBitmap();
        daypageCount++;
        updatePage(true);

    }

    private void deletePage() {
        // delete a page

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getResources().getString(R.string.delete_title));
        builder.setMessage(getResources().getString(R.string.confirm_delete));

        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            int deletePage = daypage;
            touchHelper.setRawDrawingEnabled(false);
            touchHelper.setRawDrawingRenderEnabled(false);
            touchHelper.closeRawDrawing();
            try {
                File externalFile;
                externalFile = new File(getExternalFilesDir(filepath), filename);
                if (externalFile.exists()) {
                    externalFile.delete();
                }

                for (int i = daypage; i < daypageCount; i++) {
                    System.out.println(i);
                    String newfilename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(i) + ".png";

                    String oldfilename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + String.valueOf(i + 1) + ".png";
                    externalFile = new File(getExternalFilesDir(filepath), oldfilename);
                    File newExternalFile = new File(getExternalFilesDir(filepath), newfilename);
                    if (externalFile.exists()) {
                        externalFile.renameTo(newExternalFile);

                    }

                }
                penCallback.setNeedsSave(false);
                if (daypageCount != 1)
                    daypageCount--;

                if (deletePage != 1)
                    daypage--;

                datebox = findViewById(R.id.date_text);
                datebox.setText(currentdate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + daypage + "/" + daypageCount + ")");

                filename = currentdate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + daypage + ".png";
                binding.writerview.setFilename(filename);
                binding.writerview.redrawSurface();

                startTouchHelper();
            } catch (Exception e) {
                Log.d("loadBitmap Error: ", e.getMessage(), e);
            }
            dialog.dismiss();
        });

        builder.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog alert = builder.create();
        alert.show();

    }

    private void savePages() {
        // let the user choose a time frame for export then export to pdf
        binding.writerview.redrawSurface();
        binding.writerview.saveBitmap();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialThemeDialog);
        String[] item = {getResources().getString(R.string.day), getResources().getString(R.string.month), getResources().getString(R.string.year)};

        final int[] timeframe = {0};
        builder.setTitle(getResources().getString(R.string.export_title))
                .setSingleChoiceItems(item, 0, (dialogInterface, i) -> timeframe[0] = i)
                // Set the action buttons
                .setPositiveButton(getResources().getString(R.string.ok), (dialog, id) -> {
                    try {
                        writeToPDF(timeframe[0]);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(getResources().getString(R.string.cancel), (dialog, id) -> dialog.dismiss());

        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        });
        alert.show();


    }

    private void writeToPDF(int timeframe) throws FileNotFoundException {
        // this code makes a pdf from the pages of the diary and opens it
        Toast.makeText(this, getResources().getString(R.string.export_in_progress), Toast.LENGTH_LONG).show();

        LocalDate startDate;
        LocalDate endDate;
        String outputFilename;

        switch (timeframe) {
            case 0:
                startDate = currentdate;
                endDate = currentdate;
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy")) + ".pdf";
                break;
            case 1:
                startDate = currentdate.with(firstDayOfMonth());
                endDate = currentdate.with(lastDayOfMonth());
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("MMMM-yyyy")) + ".pdf";
                break;
            case 2:
                startDate = currentdate.with(firstDayOfYear());
                endDate = currentdate.with(lastDayOfYear());
                outputFilename = "Diary-" + currentdate.format(DateTimeFormatter.ofPattern("yyyy")) + ".pdf";
                break;
            default:
                startDate = currentdate;
                endDate = currentdate;
                outputFilename = "";
        }

        PdfDocument pdfDocument = new PdfDocument();

        int pageHeight = 2200;
        int pageWidth = 1650;

        PdfDocument.PageInfo myPageInfo;
        PdfDocument.Page startPage;

        for (LocalDate printDate = startDate; !printDate.isAfter(endDate); printDate = printDate.plusDays(1)) {
            File dir = getExternalFilesDir(filepath);
            LocalDate finalPrintDate = printDate;
            File[] files = dir.listFiles((dir1, name) ->
                    name.contains(finalPrintDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

            int length = files.length;

            for (int printPage = 1; printPage <= length; printPage++) {
                String pageTitle = printDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy (")) + printPage + "/" + length + ")";
                String printfilename = printDate.format(DateTimeFormatter.ofPattern("yyyyMMdd-")) + printPage + ".png";


                File bitmapFile = new File(getExternalFilesDir(filepath), printfilename);
                if (bitmapFile.exists()) {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inScaled = true;
                    opt.inMutable = true;
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(bitmapFile), null, opt);

                    myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                    startPage = pdfDocument.startPage(myPageInfo);

                    Paint title = new Paint();

                    title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    title.setTextSize(40);
                    title.setColor(Color.WHITE);


                    Paint myPaint = new Paint();
                    myPaint.setColor(Color.rgb(0, 0, 0));
                    myPaint.setStrokeWidth(10);

                    Canvas canvas;
                    canvas = startPage.getCanvas();
                    canvas.drawRect(0, 0, pageWidth, 122, myPaint);
                    canvas.drawText(pageTitle, 110, 100, title);
                    canvas.drawBitmap(bitmap, 0, 122, null);
                    pdfDocument.finishPage(startPage);
                }
            }
        }


        ContentResolver resolver = this.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outputFilename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri path = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

        try {
            pdfDocument.writeTo(resolver.openOutputStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pdfDocument.close();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }
}
