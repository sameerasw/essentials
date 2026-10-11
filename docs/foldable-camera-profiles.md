# Foldable camera placement for Duo, Island, and Status Glance

Foldable phones can have different front cameras on the cover and inner displays. Their cutouts can also change screen edge when the device rotates. One shared offset can align an overlay with one camera and leave it over content on the other screen.

On devices advertising a hinge angle sensor or Google's dual front camera foldable feature, Duo, Island, and Status Glance placement settings show foldable controls. Single screen phones keep the existing controls. Duo already stores manual placement by display resolution; Island and Status Glance now use the same display profile fallback. Profile values are selected from the active display, so folding or unfolding switches them automatically.

Each module can optionally enable **Separate portrait and landscape placement**. When enabled, camera position and size saved in the current orientation apply only to that display and orientation. Unconfigured orientations inherit the display profile, then the original global setting. This retains existing configurations without migration. Open the settings on each screen and orientation to tune it. Auto detect still uses the active display's cutout when available; manual placement remains useful on devices that do not report the inner cutout.

**Hide on this screen in this orientation** suppresses only that module in the active display and orientation. Island and Status Glance remain hidden in landscape by default, matching their prior behavior, until enabled there. Duo remains visible by default. Rotating or folding reevaluates the active profile; hiding Duo keeps its display listener attached so it can reappear.

Display profiles currently use the active display's real pixel dimensions in orientation independent order, matching Duo's existing profile format. Devices whose inner and cover screens report exactly the same real dimensions cannot be distinguished by this key and will share a display profile. A future stable physical display identifier would allow those devices to have separate profiles.
