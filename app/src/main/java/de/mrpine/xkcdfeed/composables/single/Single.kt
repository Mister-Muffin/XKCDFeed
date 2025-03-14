package de.mrpine.xkcdfeed.composables.single

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import de.mrpine.xkcdfeed.MainViewModel
import de.mrpine.xkcdfeed.SingleComicViewModel
import de.mrpine.xkcdfeed.XKCDComic
import de.mrpine.xkcdfeed.ui.theme.Amber500
import de.mrpine.xkcdfeed.ui.theme.Gray400
import de.mrpine.xkcdfeed.ui.theme.XKCDFeedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

private const val TAG = "Single"

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun SingleViewContent(
    comic: XKCDComic,
    isFavorite: Boolean,
    dateFormat: DateFormat,
    imageLoaded: Boolean,
    getCurrentNumber: () -> Int,
    maxNumber: Int,
    setNumber: (Int) -> Unit,
    setFavorite: (XKCDComic) -> Unit,
    removeFavorite: (XKCDComic) -> Unit,
    navigateHome: () -> Unit,
    startActivity: (Intent) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val keyboardController = LocalSoftwareKeyboardController.current

    val orientation = LocalConfiguration.current.orientation
    LaunchedEffect(key1 = Unit, block = {
        if (orientation == Configuration.ORIENTATION_PORTRAIT && scaffoldState.bottomSheetState.isCollapsed)
            scaffoldState.bottomSheetState.expand()
    })

    val currentNumber = getCurrentNumber()

    var parentSize by remember { mutableStateOf(IntSize(0, 0)) }
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(contentColor = Color.White, contentPadding = PaddingValues(5.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigateHome) {
                        Icon(Icons.Default.Close, "Close")
                    }
                    IconButton(
                        onClick = { setNumber(currentNumber - 1) },
                        enabled = currentNumber > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, "Previous Comic")
                    }
                    OutlinedTextField(
                        value = currentNumber.toString(),
                        modifier = Modifier
                            .width(100.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        onValueChange = { setNumber(it.toInt()) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = Color.White,
                            focusedBorderColor = Color.White,
                            cursorColor = Color.White,
                            backgroundColor = MaterialTheme.colors.primaryVariant
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()

                        })
                    )
                    IconButton(
                        onClick = { setNumber(currentNumber + 1) },
                        enabled = currentNumber < maxNumber
                    ) {
                        Icon(Icons.Default.ArrowForward, "Next Comic")
                    }
                    IconButton(onClick = { setNumber(Random.nextInt(maxNumber + 1)) }) {
                        Icon(Icons.Default.Shuffle, "Random Comic")
                    }

                }
            }
        },
        sheetPeekHeight = 77.dp,
        sheetShape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
        sheetContent = bottomSheetContent(
            comic,
            dateFormat,
            isFavorite,
            setFavorite,
            removeFavorite,
            startActivity,
            scope
        ),
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                parentSize = layoutCoordinates.size
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if(MaterialTheme.colors.isLight) Color.White else Color.Black)
                .padding(bottom = with(LocalDensity.current) { (parentSize.height - scaffoldState.bottomSheetState.offset.value).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            if (imageLoaded) {
                val bitmap =
                    if (MaterialTheme.colors.isLight) comic.bitmapLight else comic.bitmapDark
                if (bitmap != null) {
                    ZoomableImage(
                        bitmap = bitmap.asImageBitmap(),
                        {current ->  if (current < maxNumber) setNumber(current + 1) },
                        {current -> if (current > 0) setNumber(current - 1) },
                        getCurrentNumber = getCurrentNumber
                    )
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    onSwipeLeft: (Int) -> Unit,
    onSwipeRight: (Int) -> Unit,
    getCurrentNumber: () -> Int
) {
    val scope = rememberCoroutineScope()

    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
    }

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var imageCenter by remember { mutableStateOf(Offset.Zero) }
    var transformOffset by remember { mutableStateOf(Offset.Zero) }

    fun onTransformGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        transformRotation: Float
    ) {
        offset += pan
        scale *= zoom
        rotation += transformRotation

        val x0 = centroid.x - imageCenter.x
        val y0 = centroid.y - imageCenter.y

        val hyp0 = sqrt(x0 * x0 + y0 * y0)
        val hyp1 = zoom * hyp0 * (if (x0 > 0) {
            1f
        } else {
            -1f
        })

        val alpha0 = atan(y0 / x0)

        val alpha1 = alpha0 + (transformRotation * ((2 * PI) / 360))

        val x1 = cos(alpha1) * hyp1
        val y1 = sin(alpha1) * hyp1

        transformOffset =
            centroid - (imageCenter - offset) - Offset(x1.toFloat(), y1.toFloat())
        offset = transformOffset
    }

    Box(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale != 1f) {
                            scope.launch {
                                state.animateZoomBy(1 / scale)
                            }
                            offset = Offset.Zero
                            rotation = 0f
                        } else {
                            scope.launch {
                                state.animateZoomBy(2f)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                val panZoomLock = true
                forEachGesture {
                    awaitPointerEventScope {
                        var transformRotation = 0f
                        var zoom = 1f
                        var pan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop
                        var lockedToPanZoom = false
                        var drag: PointerInputChange?
                        var overSlop = Offset.Zero

                        val down = awaitFirstDown(requireUnconsumed = false)


                        var transformEventCounter = 0
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.fastAny { it.positionChangeConsumed() }
                            var relevant = true
                            if (event.changes.size > 1) {
                                if (!canceled) {
                                    val zoomChange = event.calculateZoom()
                                    val rotationChange = event.calculateRotation()
                                    val panChange = event.calculatePan()

                                    if (!pastTouchSlop) {
                                        zoom *= zoomChange
                                        transformRotation += rotationChange
                                        pan += panChange

                                        val centroidSize =
                                            event.calculateCentroidSize(useCurrent = false)
                                        val zoomMotion = abs(1 - zoom) * centroidSize
                                        val rotationMotion =
                                            abs(transformRotation * PI.toFloat() * centroidSize / 180f)
                                        val panMotion = pan.getDistance()

                                        if (zoomMotion > touchSlop ||
                                            rotationMotion > touchSlop ||
                                            panMotion > touchSlop
                                        ) {
                                            pastTouchSlop = true
                                            lockedToPanZoom =
                                                panZoomLock && rotationMotion < touchSlop
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        val eventCentroid = event.calculateCentroid(useCurrent = false)
                                        val effectiveRotation =
                                            if (lockedToPanZoom) 0f else rotationChange
                                        if (effectiveRotation != 0f ||
                                            zoomChange != 1f ||
                                            panChange != Offset.Zero
                                        ) {
                                            onTransformGesture(
                                                eventCentroid,
                                                panChange,
                                                zoomChange,
                                                effectiveRotation
                                            )
                                        }
                                        event.changes.fastForEach {
                                            if (it.positionChanged()) {
                                                it.consumeAllChanges()
                                            }
                                        }
                                    }
                                }
                            } else if (transformEventCounter > 3) relevant = false
                            transformEventCounter++
                        } while (!canceled && event.changes.fastAny { it.pressed } && relevant)

                        do {
                            val event = awaitPointerEvent()
                            drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                change.consumePositionChange()
                                overSlop = over
                            }
                        } while (drag != null && !drag.positionChangeConsumed())
                        if (drag != null) {
                            dragOffset = Offset.Zero
                            if (scale !in 0.92f..1.08f) {
                                offset += overSlop
                            } else {
                                dragOffset += overSlop
                            }
                            if (drag(drag.id) {
                                    if (scale !in 0.92f..1.08f) {
                                        offset += it.positionChange()
                                    } else {
                                        dragOffset += it.positionChange()
                                    }
                                    it.consumePositionChange()
                                }
                            ) {
                                if (scale in 0.92f..1.08f) {
                                    val offsetX = dragOffset.x
                                    if (offsetX > 300) {
                                        onSwipeRight(getCurrentNumber())

                                    } else if (offsetX < -300) {
                                        onSwipeLeft(getCurrentNumber())
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Comic Image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .graphicsLayer(
                    scaleX = scale - 0.02f,
                    scaleY = scale - 0.02f,
                    rotationZ = rotation
                )
                .onGloballyPositioned { coordinates ->
                    val localOffset =
                        Offset(
                            coordinates.size.width.toFloat() / 2,
                            coordinates.size.height.toFloat() / 2
                        )
                    val windowOffset = coordinates.localToWindow(localOffset)
                    imageCenter = coordinates.parentLayoutCoordinates?.windowToLocal(windowOffset)
                        ?: Offset.Zero
                },
            contentScale = ContentScale.Fit
        )
    }
}


@Composable
fun bottomSheetContent(
    comic: XKCDComic,
    dateFormat: DateFormat,
    isFavorite: Boolean,
    setFavorite: (XKCDComic) -> Unit,
    removeFavorite: (XKCDComic) -> Unit,
    startActivity: (Intent) -> Unit,
    scope: CoroutineScope
): @Composable ColumnScope.() -> Unit {
    return {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colors.primary
                        )
                )

            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = comic.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                text = "(${comic.id})",
                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Text(
                            text = dateFormat.format(comic.pubDate.time),
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var icon = Icons.Outlined.StarOutline
                    var tint = Gray400

                    if (isFavorite) {
                        icon = Icons.Filled.Star
                        tint = Amber500
                    }
                    Icon(
                        icon,
                        "Star",
                        tint = tint,
                        modifier = Modifier.clickable {
                            (if (!isFavorite) setFavorite else removeFavorite)(
                                comic
                            )
                        }
                    )
                }
            }
            Text(text = comic.description)
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(onClick = {
                    (if (!isFavorite) setFavorite else removeFavorite)(
                        comic
                    )
                }, modifier = Modifier.width(140.dp)) {
                    var icon = Icons.Outlined.StarOutline
                    var tint = Gray400

                    if (isFavorite) {
                        icon = Icons.Filled.Star
                        tint = Amber500
                    }
                    Icon(
                        icon,
                        "Star",
                        tint = tint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFavorite) "Remove" else "Add")
                }
                Button(onClick = {
                    scope.launch {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "https://xkcd.com/${comic.id}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }
                }, modifier = Modifier.width(140.dp)) {
                    Icon(
                        Icons.Default.Share,
                        "Star"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun SingleViewContentStateful(
    singleViewModel: SingleComicViewModel,
    mainViewModel: MainViewModel,
    setComic: (Int) -> Unit,
    navigate: (String) -> Unit
) {
    val currentComic = singleViewModel.currentComic.value
    val currentNumber = singleViewModel.currentNumber
    val favList = mainViewModel.favoriteListFlow.collectAsState(initial = listOf())
    if (currentComic != null) {
        val favoriteList = favList.value


        SingleViewContent(
            comic = currentComic,
            isFavorite = favoriteList.contains(currentComic.id),
            dateFormat = mainViewModel.dateFormat,
            imageLoaded = singleViewModel.imageLoaded.value,
            setFavorite = mainViewModel::addFavorite,
            removeFavorite = mainViewModel::removeFavorite,
            getCurrentNumber = singleViewModel::getCurrentSingleNumber,
            setNumber = setComic,
            maxNumber = mainViewModel.latestComicNumber,
            startActivity = { mainViewModel.startActivity(it) },
            navigateHome = { navigate("mainView") }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun SinglePreview() {
    XKCDFeedTheme(darkTheme = false) {
        val comic = XKCDComic(
            "Comet Visitor",
            "https://imgs.xkcd.com/comics/comet_visitor.png",
            2524,
            Calendar.getInstance(),
            "this is a description I am too lazy to copy",
            rememberCoroutineScope(),
            {}
        )
        SingleViewContent(
            comic = comic,
            isFavorite = true,
            DateFormat.getDateInstance(),
            false,
            {2524},
            2526,
            {},
            {},
            {},
            {},
            {})

    }
}