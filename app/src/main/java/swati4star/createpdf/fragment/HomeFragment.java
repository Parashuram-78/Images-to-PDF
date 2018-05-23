package swati4star.createpdf.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.morphingbutton.MorphingButton;
import com.gun0912.tedpicker.ImagePickerActivity;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;

import butterknife.ButterKnife;
import id.zelory.compressor.Compressor;
import swati4star.createpdf.R;
import swati4star.createpdf.adapter.ViewFilesAdapter;

import static java.util.Collections.singletonList;


/**
 * HomeFragment fragment to start with creating PDF
 */
public class HomeFragment extends Fragment {

    private static final int INTENT_REQUEST_GET_IMAGES = 13;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT = 1;
    private static int mImageCounter = 0;
    private Activity activity;
    private ArrayList<String> imagesUri;
    private ArrayList<String> tempUris;
    private String path;
    private String filename;
    private MorphingButton createPdf;
    private MorphingButton openPdf;
    private MorphingButton cropImages;
    private int mMorphCounter1 = 1;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, root);

        //initialising variables
        imagesUri = new ArrayList<>();
        tempUris = new ArrayList<>();
        MorphingButton addImages = root.findViewById(R.id.addImages);
        cropImages = root.findViewById(R.id.cropImages);
        createPdf = root.findViewById(R.id.pdfCreate);
        openPdf = root.findViewById(R.id.pdfOpen);


        morphToSquare(createPdf, integer());
        openPdf.setVisibility(View.GONE);

        addImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAddingImages();
            }
        });

        cropImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImages();
            }
        });

        createPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPdf();
            }
        });

        openPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPdf();
            }
        });

        // Get runtime permissions if build version >= Android M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
            }
        }

        return root;
    }

    // Adding Images to PDF
    private void startAddingImages() {
        // Check if permissions are granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
            } else {
                selectImages();
            }
        } else {
            selectImages();
        }
    }

    private void cropImages() {
        if (tempUris.size() == 0) {
            Toast.makeText(activity, R.string.toast_no_images, Toast.LENGTH_SHORT).show();
            return;
        }
        next();
    }

    private void next() {
        if (mImageCounter != tempUris.size()) {
            CropImage.activity(Uri.fromFile(new File(tempUris.get(mImageCounter))))
                    .setActivityMenuIconColor(color(R.color.colorPrimary))
                    .setInitialCropWindowPaddingRatio(0)
                    .setAllowRotation(true)
                    .setActivityTitle(getString(R.string.cropImage_activityTitle) + (mImageCounter + 1))
                    .start(activity, this);
        }
    }

    // Create Pdf of selected images
    @SuppressWarnings("unchecked")
    private void createPdf() {
        if (imagesUri.size() == 0) {
            if (tempUris.size() == 0) {
                Toast.makeText(activity, R.string.toast_no_images, Toast.LENGTH_LONG).show();
                return;
            } else
                imagesUri = (ArrayList<String>) tempUris.clone();
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.creating_pdf)
                .content(R.string.enter_file_name)
                .input(getString(R.string.example), null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if (input == null || input.toString().trim().equals("")) {
                            Toast.makeText(activity, R.string.toast_name_not_blank, Toast.LENGTH_LONG).show();
                        } else {
                            filename = input.toString();

                            new CreatingPdf().execute();

                            if (mMorphCounter1 == 0)
                                mMorphCounter1++;
                        }
                    }
                })
                .show();
    }

    private void openPdf() {
        File file = new File(path);
        Intent target = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(activity, "com.swati4star.shareFile", file);

        target.setDataAndType(uri, getString(R.string.pdf_type));
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = Intent.createChooser(target, getString(R.string.open_file));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.toast_no_pdf_app, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Opens ImagePickerActivity to select Images
     */
    private void selectImages() {
        Intent intent = new Intent(activity, ImagePickerActivity.class);

        //add to intent the URIs of the already selected images
        //first they are converted to Uri objects
        ArrayList<Uri> uris = new ArrayList<>(tempUris.size());
        for (String stringUri : tempUris) {
            uris.add(Uri.fromFile(new File(stringUri)));
        }
        // add them to the intent
        intent.putExtra(ImagePickerActivity.EXTRA_IMAGE_URIS, uris);

        startActivityForResult(intent, INTENT_REQUEST_GET_IMAGES);
    }

    /**
     * Called after user is asked to grant permissions
     *
     * @param requestCode  REQUEST Code for opening permissions
     * @param permissions  permissions asked to user
     * @param grantResults bool array indicating if permission is granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectImages();
                    Toast.makeText(activity, R.string.toast_permissions_given, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, R.string.toast_insufficient_permissions, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Called after ImagePickerActivity is called
     *
     * @param requestCode REQUEST Code for opening ImagePickerActivity
     * @param resultCode  result code of the process
     * @param data        Data of the image selected
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_GET_IMAGES && resultCode == Activity.RESULT_OK) {

            tempUris.clear();
            ArrayList<Uri> imageUris = data.getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);
            for (int i = 0; i < imageUris.size(); i++)
                tempUris.add(imageUris.get(i).getPath());
            Toast.makeText(activity, R.string.toast_images_added, Toast.LENGTH_LONG).show();
            morphToSquare(createPdf, integer());
            cropImages.setVisibility(View.VISIBLE);

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            switch (resultCode)
            {
                case Activity.RESULT_OK :
                    Uri resultUri = result.getUri();
                    imagesUri.add(resultUri.getPath());
                    Toast.makeText(activity, R.string.toast_imagecropped, Toast.LENGTH_LONG).show();
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE:
                    Exception error = result.getError();
                    Toast.makeText(activity, R.string.toast_error_getCropped, Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                default:
                    imagesUri.add(tempUris.get(mImageCounter));
            }
            morphToSquare(createPdf, integer());
            mImageCounter++;
            next();
        }
    }

    /**
     * Converts morph button ot square shape
     *
     * @param btnMorph the button to be converted
     * @param duration time period of transition
     */
    private void morphToSquare(final MorphingButton btnMorph, int duration) {
        MorphingButton.Params square = MorphingButton.Params.create()
                .duration(duration)
                .cornerRadius(dimen(R.dimen.mb_corner_radius_2))
                .width(dimen(R.dimen.mb_width_200))
                .height(dimen(R.dimen.mb_height_56))
                .color(color(R.color.mb_blue))
                .colorPressed(color(R.color.mb_blue_dark))
                .text(getString(R.string.mb_button));
        btnMorph.morph(square);
    }

    /**
     * Converts morph button into success shape
     *
     * @param btnMorph the button to be converted
     */
    private void morphToSuccess(final MorphingButton btnMorph) {
        MorphingButton.Params circle = MorphingButton.Params.create()
                .duration(integer())
                .cornerRadius(dimen(R.dimen.mb_height_56))
                .width(dimen(R.dimen.mb_height_56))
                .height(dimen(R.dimen.mb_height_56))
                .color(color(R.color.mb_green))
                .colorPressed(color(R.color.mb_green_dark))
                .icon(R.drawable.ic_done);
        btnMorph.morph(circle);
    }

    private int integer() {
        return getResources().getInteger(R.integer.mb_animation);
    }

    private int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }

    private int color(@ColorRes int resId) {
        return getResources().getColor(resId);
    }

    /**
     * An async task that converts selected images to Pdf
     */
    @SuppressLint("StaticFieldLeak")
    class CreatingPdf extends AsyncTask<String, String, String> {

        // Progress dialog
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                .title(R.string.please_wait)
                .content(R.string.populating_list)
                .cancelable(false)
                .progress(true, 0);
        final MaterialDialog dialog = builder.build();


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    HomeFragment.this.getString(R.string.pdf_dir);

            File folder = new File(path);
            if (!folder.exists()) {
                boolean success = folder.mkdir();
                if (!success) {
                    Toast.makeText(activity, R.string.toast_folder_not_created, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            path = path + filename + HomeFragment.this.getString(R.string.pdf_ext);

            Log.v("stage 1", "store the pdf in sd card");

            Document document = new Document(PageSize.A4, 38, 38, 50, 38);

            Log.v("stage 2", "Document Created");

            Rectangle documentRect = document.getPageSize();

            try {
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));

                Log.v("Stage 3", "Pdf writer");

                document.open();

                Log.v("Stage 4", "Document opened");

                for (int i = 0; i < imagesUri.size(); i++) {

                    Bitmap bmp = new Compressor(activity)
                            .setQuality(70)
                            .setCompressFormat(Bitmap.CompressFormat.PNG)
                            .compressToBitmap(new File(imagesUri.get(i)));

                    Image image = Image.getInstance(imagesUri.get(i));


                    if (bmp.getWidth() > documentRect.getWidth()
                            || bmp.getHeight() > documentRect.getHeight()) {
                        //bitmap is larger than page,so set bitmap's size similar to the whole page
                        image.scaleAbsolute(documentRect.getWidth(), documentRect.getHeight());
                    } else {
                        //bitmap is smaller than page, so add bitmap simply.
                        image.scaleAbsolute(bmp.getWidth(), bmp.getHeight());
                    }

                    Log.v("Stage 6", "Image path adding");

                    image.setAbsolutePosition(
                            (documentRect.getWidth() - image.getScaledWidth()) / 2,
                            (documentRect.getHeight() - image.getScaledHeight()) / 2);
                    Log.v("Stage 7", "Image Alignments");

                    image.setBorder(Image.BOX);

                    image.setBorderWidth(15);

                    document.add(image);

                    document.newPage();
                }

                Log.v("Stage 8", "Image adding");

                document.close();

                Log.v("Stage 7", "Document Closed" + path);
            } catch (Exception e) {
                e.printStackTrace();
            }

            document.close();
            ResetValues();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            openPdf.setVisibility(View.VISIBLE);
            Snackbar.make(Objects.requireNonNull(getActivity()).findViewById(android.R.id.content)
                    , R.string.snackbar_pdfCreated
                    , Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_viewAction, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ArrayList<File> list = new ArrayList<>(singletonList(new File(path)));
                            ViewFilesAdapter filesAdapter = new ViewFilesAdapter(activity, list);
                            filesAdapter.openFile(path);
                        }
                    }).show();
            dialog.dismiss();
            morphToSuccess(createPdf);
        }
    }

    private void ResetValues()
    {
        imagesUri.clear();
        tempUris.clear();
        mImageCounter = 0;
    }

}