#include <jni.h>
#include "FrmsubsImpl.h"


JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getshm
  (JNIEnv *env, jobject obj) {
  
  int ret;
  ret = 0;
      
  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getnumfrm
  (JNIEnv *env, jobject obj) {

  int ret;
  ret = getnumfrm();

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getcurfrm
  (JNIEnv *env, jobject obj) {

  int ret = getcurfrm();

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getfrmsize
  (JNIEnv *env, jobject obj, jintArray frame_a, jintArray linsize_a, jintArray elesize_a) {

  int ret;

  jint *cf = (*env)->GetIntArrayElements(env, frame_a, 0);
  jint *ls = (*env)->GetIntArrayElements(env, linsize_a, 0);
  jint *es = (*env)->GetIntArrayElements(env, elesize_a, 0);

  ret = getfrmsize(cf, ls, es);

  (*env)->ReleaseIntArrayElements(env, frame_a, cf, 0);
  (*env)->ReleaseIntArrayElements(env, linsize_a, ls, 0);
  (*env)->ReleaseIntArrayElements(env, elesize_a, es, 0);

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getdirty
  (JNIEnv *env, jobject obj, jint frame) {

  int ret;

  ret = getdirty(frame);

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getfrm
  (JNIEnv *env, jobject obj, jint frame, jint linsize, jint elesize, 
   jcharArray img, jintArray stretchtab, jintArray colortab, jintArray graphicstab) {

  int ret;

  jchar *jimg = (*env)->GetCharArrayElements(env, img, 0);
  jint *jstab = (*env)->GetIntArrayElements(env, stretchtab, 0);
  jint *jctab = (*env)->GetIntArrayElements(env, colortab, 0);
  jint *jgtab = (*env)->GetIntArrayElements(env, graphicstab, 0);

  ret = getfrm(frame, linsize, elesize, jimg, jstab, jctab, jgtab);

  (*env)->ReleaseCharArrayElements(env, img, jimg, 0);
  (*env)->ReleaseIntArrayElements(env, stretchtab, jstab, 0);
  (*env)->ReleaseIntArrayElements(env, colortab, jctab, 0);
  (*env)->ReleaseIntArrayElements(env, graphicstab, jgtab, 0);

  return (jint) ret;
}


JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getgrasize
  (JNIEnv *env, jobject obj, jint frame, jintArray npts_a, jintArray nblocks_a, jintArray mask_a) {

  int ret;
  int npts;
  int nblocks;
  int mask;

  ret = getgrasize(frame, &npts, &nblocks, &mask);

  if (ret >= 0) {
    jint *np = (*env)->GetIntArrayElements(env, npts_a, 0);
    jint *nb = (*env)->GetIntArrayElements(env, nblocks_a, 0);
    jint *mm = (*env)->GetIntArrayElements(env, mask_a, 0);
    np[0] = npts;
    nb[0] = nblocks;
    mm[0] = mask;
    (*env)->ReleaseIntArrayElements(env, npts_a, np, 0);
    (*env)->ReleaseIntArrayElements(env, nblocks_a, nb, 0);
    (*env)->ReleaseIntArrayElements(env, mask_a, mm, 0);
  }
  return (jint) ret;
}


JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getgra
  (JNIEnv *env, jobject obj, jint frame, jint npts, jintArray graphics) {

  int ret;

  jint *jgra = (*env)->GetIntArrayElements(env, graphics, 0);
  ret = getgra(frame, npts, jgra);
  (*env)->ReleaseIntArrayElements(env, graphics, jgra, 0);

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_getdir
  (JNIEnv *env, jobject obj, jint frame, jintArray frmdir) {

  int ret;

  jint *jfdir = (*env)->GetIntArrayElements(env, frmdir, 0);
  ret = getdir(frame, jfdir);
  (*env)->ReleaseIntArrayElements(env, frmdir, jfdir, 0);

  return (jint) ret;
}

JNIEXPORT jint JNICALL Java_ucar_unidata_data_imagery_mcidas_FrmsubsImpl_detshm
  (JNIEnv *env, jobject obj) {
  
  int ret;
  ret = 0;
      
  return (jint) ret;
}

