import glob
import json
import os
import re
import sys
import zipfile

DOCS = os.path.dirname(os.path.abspath(__file__))
IMAGES = 'images/blocks'

METHOD_GROUPS = [
    ('Setup and playback', 'Shows the player and controls playback.',
     ['Create', 'Play', 'PlayFrom', 'Resume', 'Pause', 'Stop', 'Replay', 'SeekTo', 'SeekToLiveEdge']),
    ('Queue', 'Builds and moves through the queue. Indexes start at 1.',
     ['AddToQueue', 'InsertAt', 'RemoveAt', 'MoveItem', 'PlayAt', 'SeekToItem', 'Next', 'Previous', 'ClearQueue']),
    ('Media items', 'Media items describe one media and how to play it, as a dictionary with keys from the field '
     'dropdowns. See [Media items](../README.md#media-items).',
     ['SetMediaItems', 'AddMediaItem', 'AddMediaItems', 'ReplaceMediaItem', 'GetMediaItemAt', 'SetMediaItemField',
      'SetDrmConfigurationField', 'SetSubtitleConfigurationField', 'SetClippingConfigurationField', 'SetThumbnailsField']),
    ('Fullscreen, dialog and frames', 'Where the player is shown, and saving frames.',
     ['EnterFullscreen', 'ExitFullscreen', 'ShowAsDialog', 'CloseDialog', 'MoveTo', 'CaptureFrame']),
    ('Tracks, chapters and requests', 'Quality, audio and subtitle tracks, chapters, and HTTP headers.',
     ['SelectQuality', 'SelectAudioTrack', 'SelectSubtitle', 'AddChapter', 'ClearChapters', 'SetHeader', 'ClearHeaders']),
    ('Audio and volume', "The equalizer and the phone's media volume.",
     ['UseEqualizerPreset', 'SetBandLevel', 'BandLevel', 'IncreaseDeviceVolume', 'DecreaseDeviceVolume']),
    ('Downloads and cache', 'Offline playback and the stream cache.',
     ['Download', 'RemoveDownload', 'RemoveAllDownloads', 'IsDownloaded', 'DownloadProgress', 'ClearCache']),
]

PROPERTY_GROUPS = [
    ('Appearance', 'Also in the designer, under Appearance.', lambda p: p.get('category') == 'Appearance'),
    ('Behavior', 'Also in the designer, under Behavior.', lambda p: p.get('category') == 'Behavior'),
    ('Advanced', 'Also in the designer, under Advanced.', lambda p: p.get('category') == 'Advanced'),
    ('Phone volume', "The phone's media volume, shared with other apps.",
     lambda p: p['rw'] == 'read-write' and not p.get('category')),
    ('Read-only', 'State of the player and the current media.', lambda p: p['rw'] == 'read-only'),
]


def is_deprecated(entry):
    return str(entry.get('deprecated')).lower() == 'true'


def sentence(text):
    text = text.strip()
    return text if text.endswith(('.', '!', '?')) else text + '.'


def anchor(title):
    return re.sub(r'[^\w\- ]', '', title.lower()).replace(' ', '-')


class Reference:

    def __init__(self, component):
        self.component = component
        self.instance = f'{component["name"]}1'
        self.images = []

    def image(self, name, alt):
        self.images.append(name)
        return f'![{alt}]({IMAGES}/{name}.png)'

    def block(self, name, description):
        return f'{self.image(f"{self.instance}-{name}", name)}\n\n**{name}:** {sentence(description)}'

    def property(self, prop):
        shots = [self.image(f'{self.instance}-get{prop["name"]}', f'get {prop["name"]}')]
        if prop['rw'] == 'read-write':
            shots.append(self.image(f'{self.instance}-set{prop["name"]}', f'set {prop["name"]}'))
        return ' '.join(shots) + f'\n\n**{prop["name"]}** ({prop["rw"]}): {sentence(prop["description"])}'

    def dropdowns(self):
        found = {}
        for entry in self.component['events'] + self.component['methods'] + self.component['blockProperties']:
            for data in [entry.get('helper', {}).get('data')] + [p.get('helper', {}).get('data') for p in entry.get('params', [])]:
                if data:
                    found[data['key']] = data
        return [f'{self.image(f"helper-{key}", key)}\n\n**{key}:** ' + ', '.join(o['name'] for o in data['options'])
                for key, data in sorted(found.items())]

    def markdown(self):
        methods = {m['name']: m for m in self.component['methods'] if not is_deprecated(m)}
        grouped = {name for _, _, names in METHOD_GROUPS for name in names}
        if grouped != set(methods):
            sys.exit(f'Blocks without a group, or groups naming missing blocks: {sorted(grouped ^ set(methods))}')
        properties = [p for p in self.component['blockProperties'] if not is_deprecated(p)]
        sections = []
        for title, intro, names in METHOD_GROUPS:
            sections.append((f'Functions: {title}', intro, [self.block(n, methods[n]['description']) for n in names]))
        sections.append(('Events', 'Events fire on the main thread, after Screen.Initialize.',
                         [self.block(e['name'], e['description']) for e in self.component['events']]))
        for title, intro, keep in PROPERTY_GROUPS:
            sections.append((f'Properties: {title}', intro, [self.property(p) for p in properties if keep(p)]))
        sections.append(('Dropdowns', 'Choices for blocks and media item fields.', self.dropdowns()))
        deprecated = sorted(m['name'] for m in self.component['methods'] if is_deprecated(m))

        lines = [
            '# Blocks',
            '',
            f'Every block of {self.component["name"]} {self.component["versionName"]}. Each image contains its block, '
            'so you can drag it straight into your workspace.',
            '',
        ]
        lines += [f'- [{title}](#{anchor(title)})' for title, _, _ in sections]
        for title, intro, entries in sections:
            lines += ['', f'## {title}', '', intro, '', '\n\n'.join(entries)]
        lines += ['', '## Deprecated', '',
                  'Hidden from the blocks drawer and kept working for existing projects. They change the most '
                  'recently added media; use media items instead: ' + ', '.join(f'`{n}`' for n in deprecated) + '.', '']
        return '\n'.join(lines)


def main(aix_path):
    with zipfile.ZipFile(aix_path) as aix:
        component = next(json.loads(aix.read(n))[0] for n in aix.namelist() if n.endswith('/components.json'))
    reference = Reference(component)
    text = reference.markdown()
    missing = [n for n in reference.images if not os.path.exists(os.path.join(DOCS, IMAGES, f'{n}.png'))]
    if missing:
        sys.exit(f'Missing block images: {", ".join(missing)}')
    with open(os.path.join(DOCS, 'blocks.md'), 'w') as f:
        f.write(text)


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'out'
    main(glob.glob(os.path.join(out, '*.aix'))[0])
