#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/shm.h>
#include <math.h>

typedef struct
{
    /*
     * processes which modify the contents of the frame object
     * and want to apprise the image application(s) of the fact
     * set 'dirty' to -1 (all one bits)
     *
     * image applications can use individual bits and reset them
     * at will
     */
    int dirty;

    /*
     *  Bounding rectangle.  Line and element start at 1
     * (1,1) is upper left corner
     *  0 means the rectangle is empty
     */
    int ul_line;     /* upper left */
    int ul_elem;
    int lr_line;     /* lower right*/
    int lr_elem;

    /*
     * Dirty flag and bounding box for McV interface
    */

    int McV_dirty;
    int McV_ul_line;     /* upper left */
    int McV_ul_elem;
    int McV_lr_line;     /* lower right*/
    int McV_lr_elem;

    /*
     * stretch table always has 256 entries from 0 - (maxcolors-1)
     */
    int stretch_table[256];

    /*
     * color table has up to 256 entries
     */
    unsigned int color_table[256];

    /*
     * color table for the independant graphics
     *
     * while not exactly associated with this frame object,
     * there are the right number of them, so it is a
     * convenient place to put it
     */
    unsigned int graphics_table[256];

    unsigned int graphics_mask;

} M0frameflags;

#define GRAPHICS_POINTS_PER_BLOCK 254  /* total size will be (254+2)*4 = 1K per block */

typedef struct
{
  int NumberOfPoints;                         /* number of points stored in this block */
  int PointsList[GRAPHICS_POINTS_PER_BLOCK];  /* stored as bits 8-31 are offset into the frame 0-7 color */
  int NextBlock;                              /* Index pointing to the next block */
} GraphicsPointsBlock;

static int *m0posuc;
int lastfrm=0;

int
getshm()
{
/* getshm - Get shared memory
 *
 * Input:   none
 * Output: *m0posuc = starting address of shared memory
 *
 * Return value = 0  OK
 *              < 0  Error, shared memory not attached
*/
  char *valptr;                    /* pointer to value of MCENV_POSUC */
  int val=0;                       /* UC shared memory key */

/**********************************************************************/

/* Retrieve environmental variable containing shared memory key */
  valptr = getenv("MCENV_POSUC");
  if (!valptr)  {
      printf("Unable to locate MCENV_POSUC\n");
      return -1;
  }
  val = atoi(valptr);

/* attach shared memory */
  m0posuc = (int *)shmat(val,0,0);

  if (m0posuc<(int *)0)  {
     printf("Unable to attach shared memory\n");
     return -2;
  }
  return 0;
}

int detshm()
{
/* detshm - Detach shared memory
*/
                                                                                               
/**********************************************************************/
                                                                                               
  shmdt((void*)m0posuc);
  return 0;
}

int
getnumfrm()
{
/* getnumfrm - Get current number of frames
 *
 * Input:  none
 * Output: none
 *
 * Return value = number of frames
*/
/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

/* get number of frames */
  return m0posuc[13];
}

int
getcurfrm()
{
/* getcurfrm - Get current frame number
 *
 * Input:  none
 * Output: none
 * 
 * Return value = currently displayed frame number
*/
/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

/* get current frame number */
  return m0posuc[51];
}

int
getfrmsize(int *frame, int *linsiz, int *elesiz)
{
/* getfrmsize - Get current frame number and dimensions
 *
 * Input:  none
 * Output: frame        = frame number
 *         linsiz       = line dimension of frame
 *         elesiz       = element dimension of frame
 *
 * Return value = 0  OK
 *              < 0  Error
*/
  int istat;              /* status return value */

/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

/* get current frame number and dimensions */
  if (*frame == -1)  *frame = m0posuc[51];
  if (*frame>m0posuc[13])  {
     printf("Invalid frame number %d\n",*frame);
     detshm();
     return -1;
  }
  
  *elesiz = m0posuc[3000+*frame]/65536;
  *linsiz = m0posuc[3000+*frame]%65536;

  return 0;
}

int
getgrasize(int frame, int *npts, int *nblocks, int *mask)
{
/* getgrasize - Get number of graphics points associated with current frame
 *
 * Input:  frame        = frame number
 * Output: npts         = number of graphics points
 *         nblocks      = number of blocks in graphics linked list
 *         mask         = color level mask
 *
 * Return value = 0  OK
 *              < 0  Error
*/
  int istat;                       /* status return value */
  int GraphicsFrame;               /* which frame are we attempting to draw */
  char frmoff=0;
  M0frameflags *tFlags;            /* pointer to flags and frame info */
  GraphicsPointsBlock *GraphicsBuffer;  /* access to block of points */
  int j;                           /* counter */
  GraphicsPointsBlock *Head;       /* traverse the linked list */

/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

  *npts=0;
  *nblocks=0;
  *mask=0;

  GraphicsFrame = m0posuc[10000+frame];
  frmoff = m0posuc[2000 + GraphicsFrame];
  tFlags = (M0frameflags *)(unsigned char *)m0posuc;
  tFlags += frmoff;
  *mask=tFlags->graphics_mask;

/* attach graphics shared memory */
  GraphicsBuffer=(GraphicsPointsBlock *)shmat(m0posuc[505],0,0);
  if (GraphicsBuffer<(GraphicsPointsBlock *)0)  {
     printf("\nUnable to attach graphics shared memory\n");
     detshm();
     return -1;
  }

/* count number of graphics points */
  j = m0posuc[8000+GraphicsFrame];
  while (j != -1)  {
     Head = &GraphicsBuffer[j];
     *npts+=Head->NumberOfPoints;
     (*nblocks)++;
     j=Head->NextBlock;
  }

/* detach graphics shared memory */
  shmdt((void*)GraphicsBuffer);
  return 0;
}

int
getfrm(int frame, int linsize, int elesize, unsigned char img[],
       int stretchtab[], int colortab[], int graphicstab[] )
{
/* getfrm - Get frame data
 *
 * Input:  frame        = frame number
 *         linsize      = line dimension of frame
 *         elesize      = element dimension of frame
 * Output: img          = array containing frame data
 *         stretchtab   = array containing stretch table
 *         colortab     = array containing color table
 *         graphicstab  = array containing graphics table
 *
 * Return value = 0  OK
 *              < 0  Error, img array not allocated
*/
  int istat;              /* status return value */
  unsigned char *byte1=0;
  unsigned char *byte2=0;
  int tabsize;            /* size in bytes of M0frameflags */
  unsigned char *tabptr;  /* pointer to color tables */
  int i,j;

/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

/* get starting address of frame data */
  byte1 = (unsigned char *)m0posuc;
  byte1 += m0posuc[2000+frame];

  tabsize = sizeof(M0frameflags);

/* starting address of color tables */
  tabptr = byte1 + 40;
  byte2 = byte1 + tabsize;
/*  byte2 += elesize*12;  ** skip over doc at bottom of frame */
  byte2 += elesize;

/* copy frame data into img */
/*  memcpy(img,(char *)byte2,(linsize-12)*(elesize)); */
  memcpy(img,(char *)byte2,(linsize)*(elesize));

/* copy stretch table */
  memcpy(stretchtab,tabptr,1024);
  tabptr += 1024;

/* copy color table */
  memcpy(colortab,tabptr,1024);

  tabptr += 1024;

/* copy graphics table */
  memcpy(graphicstab,tabptr,1024);

  return 0;
}

int
getdirty(int frame)
{
/* getdirty - Get "dirty" flag
 *
 * Input:  frame  = frame number
 * Output: dirty  = dirty flag ( = -1 if the frame contents have changed)
 *
 * Return value = 0  OK
 *              < 0  Error, img array not allocated
 */
  int istat;              /* status return value */
  unsigned char *byte1=0;

/**********************************************************************/

  int linsize,elesize;
  int dirty;
  int reset=0;

  getfrmsize(&frame, &linsize, &elesize);

  if (m0posuc == (int *)0)  getshm();

  byte1 = (unsigned char *)m0posuc;
  byte1 += m0posuc[2000+frame];
  byte1 += 5*(sizeof(int));  /* Mc-V dirty flag */
  memcpy(&dirty,byte1,sizeof(int));

  if ((frame != lastfrm) || (dirty != 0)) {
    lastfrm = frame;
    memcpy(byte1,&reset,sizeof(int));  /* reset Mc-V dirty flag */
    return -1;
  }

  return 0;
}

int
getgra(int frame, int npts, int gra[])
{
/* getgra - Get graphics data
 *
 * Input:  frame  = frame number
 *         npts   = number of graphics points
 * Output: gra    = array containing graphics data
 *
 * Return value = 0  OK
 *              < 0  Error, graphics shared memory not attached
 *                          and gra array not allocated
*/
  int istat;              /* status return value */
  int GraphicsFrame;               /* which frame are we attempting to draw */
  GraphicsPointsBlock *GraphicsBuffer;  /* access to block of points */
  int i,j;                         /* counters */
  GraphicsPointsBlock *Head;       /* traverse the linked list */
  unsigned int *Buffer;            /* Link to list of points in each block */
  int ptcount=0;

/**********************************************************************/

  if (m0posuc == (int *)0)  getshm();

/* initialize gra array */
  memset(gra,(char)0,npts*sizeof(int));

/* attach graphics shared memory */
  GraphicsBuffer=(GraphicsPointsBlock *)shmat(m0posuc[505],0,0);
  if (GraphicsBuffer<(GraphicsPointsBlock *)0)  {
     printf("\nUnable to attach graphics shared memory\n");
     detshm();
     return -1;
  }

/* copy graphics data into gra */
  GraphicsFrame = m0posuc[10000+frame];
  j = m0posuc[8000+GraphicsFrame];

  while (j != -1)  {
     /* set up pointers to the buffers */
     Head = &GraphicsBuffer[j];
     Buffer = (unsigned int*)Head->PointsList;
     for (i=0; i<Head->NumberOfPoints; i++)  {
         gra[ptcount]=*Buffer;
         ++ptcount;
         Buffer++;
     }
     j=Head->NextBlock;
     if (ptcount>npts)  j=-1;
  }

/* detach graphics shared memory */
  shmdt((void*)GraphicsBuffer);
  return 0;
}

int
filename(int frame,char fn[])
{
/* filename - get the name of the frame directory for a frame
 *
 * Input:  frame = frame number
 * Output: fn = string containing the name of the file directory
 *              in the form <path>/Frame.<frame_number>.0
 *
 * Return value = 0  OK
 *              < 0  Error
 */

  char *filepath = 0;

/**********************************************************************/

/* trivial frame validation */
  if (frame < 1)  return -1;

/* prefix path name, last directory in MCPATH */
   char *mcpath = getenv("MCPATH");
   if (!mcpath)  return -1;
 
   filepath = strrchr(mcpath, ':');
   if (!filepath)  filepath = mcpath;
   else  filepath++;

   sprintf(fn,"%s/",filepath);
  return 0;
}

int
getdir( int frame, int frmdir[])
{
/* getdir - Get frame directory, including navigation block
 *          in words 64-703
 *
 * Input:  frame  = frame number
 * Output: frmdir = array containing frame directory
 *
 * Return value = 0  OK
 *              < 0  Error, frame directory not read
*/
  char fsrc[256];                  /* frame directory file name */
  FILE *fs=0;
  int istat=0;

/**********************************************************************/

/* get name of frame directory file for frame */
  istat = filename(frame,fsrc);
  sprintf(fsrc,"%sFrame%d.0",fsrc,frame);
  if (istat<0)  return istat;
/* open frame directory file */
  fs = fopen(fsrc,"rb");
  if (!fs)  return -1;
/* rewind frame directory file */
  istat = fseek(fs,0L,SEEK_SET);
  if (istat<0)  {
     fclose(fs);
     return istat;
  }

/* read contents of frame directory file into frmdir */
  if (fread(frmdir,sizeof(int),704,fs)<704)  {
     fclose(fs);
     return -1;
  }

  fclose(fs);

  return 0;
}
